"""Gera as versões de APRESENTAÇÃO dos diagramas de sequência UC02 e UC03
no estilo da skill diagram-design (tokens default, HTML + SVG inline).

A fonte da verdade do design continua sendo docs/uml/*.puml (PlantUML).
Estes HTMLs são uma releitura editorial para os slides: se um .puml mudar,
atualize este script e rode de novo.

Uso: python3 gerar_sequencias.py <pasta-de-saida>
Saída: sequencia-uc02.html e sequencia-uc03.html
"""
import sys
from html import escape

PAPER, PAPER2, INK, MUTED, SOFT = "#f5f5f5", "#ececec", "#2d3142", "#4f5d75", "#5a6580"
ACCENT, ACCENT_TINT, LINK = "#bf4520", "rgba(191,69,32,0.08)", "#2e5aa8"
SANS, MONO = "'Geist', system-ui, sans-serif", "'Geist Mono', ui-monospace, monospace"

# tratamento por camada (style-guide: Node type -> treatment)
TREAT = {
    "ator":   dict(fill="rgba(79,93,117,0.10)", stroke=SOFT, tag="ATOR"),
    "ui":     dict(fill="rgba(79,93,117,0.10)", stroke=SOFT, tag="UI"),
    "vm":     dict(fill="rgba(79,93,117,0.10)", stroke=SOFT, tag="VIEWMODEL"),
    "uc":     dict(fill="#ffffff", stroke=INK, tag="USE CASE"),
    "focal":  dict(fill=ACCENT_TINT, stroke=ACCENT, tag="USE CASE"),
    "dom":    dict(fill="#ffffff", stroke=INK, tag="DOMÍNIO"),
    "fab":    dict(fill="#ffffff", stroke=INK, tag="FACTORY"),
    "repo":   dict(fill="rgba(45,49,66,0.05)", stroke=MUTED, tag="REPOSITÓRIO"),
}

CHAR_MONO = 6.0   # largura aprox. de um caractere Geist Mono a 10px
CHAR_SANS = 6.4   # Geist 600 a 11px


def text(x, y, s, size=10, fill=INK, family=MONO, anchor="middle", weight=None, ls=None, italic=False):
    w = f' font-weight="{weight}"' if weight else ""
    l = f' letter-spacing="{ls}"' if ls else ""
    it = ' font-style="italic"' if italic else ""
    return (f'<text x="{x}" y="{y}" fill="{fill}" font-size="{size}" font-family="{family}"'
            f' text-anchor="{anchor}"{w}{l}{it}>{escape(s)}</text>')


class Painel:
    def __init__(self, slug, titulo, desc, lifelines, largura_caixa=136, passo=152, margem=72):
        self.slug, self.titulo, self.desc = slug, titulo, desc
        self.ll = lifelines            # lista de (id, nome, sub, tipo)
        self.bw, self.passo, self.margem = largura_caixa, passo, margem
        self.x = {lid: margem + largura_caixa / 2 + i * passo for i, (lid, *_ ) in enumerate(lifelines)}
        self.w = int(margem * 2 + largura_caixa + (len(lifelines) - 1) * passo)
        self.top = 120               # base das caixas
        self.y = self.top + 40       # cursor de tempo
        self.camada_seta, self.camada_rot, self.camada_frag, self.camada_ativ = [], [], [], []
        self.n = 0

    # ---------- mensagens ----------
    def msg(self, a, b, rotulo, kind="call", num=True, linhas=None, gap=36):
        """kind: call | ret | acc (headline) | exc (exceção) """
        self.y += gap
        y = self.y
        xa, xb = self.x[a], self.x[b]
        dirn = 1 if xb > xa else -1
        x1, x2 = xa + 4 * dirn, xb - 6 * dirn
        cor = {"call": MUTED, "ret": MUTED, "acc": ACCENT, "exc": ACCENT}[kind]
        mk = {"call": "arrow", "ret": "arrow", "acc": "arrow-accent", "exc": "arrow-accent"}[kind]
        dash = ' stroke-dasharray="5,4"' if kind in ("ret", "exc") else ""
        sw = "1.4" if kind == "acc" else "1.2"
        self.camada_seta.append(f'<line x1="{x1}" y1="{y}" x2="{x2}" y2="{y}" stroke="{cor}" stroke-width="{sw}"{dash} marker-end="url(#{self.slug}-{mk})"/>')
        if num:
            self.n += 1
            rotulo = f"{self.n}  {rotulo}"
        partes = linhas or [rotulo]
        if linhas and num:
            partes = [f"{self.n}  {linhas[0]}"] + linhas[1:]
        cx = (xa + xb) / 2
        for i, p in enumerate(reversed(partes)):
            ty = y - 8 - i * 13
            wlab = len(p) * CHAR_MONO + 12
            self.camada_rot.append(f'<rect x="{cx - wlab/2:.0f}" y="{ty - 10}" width="{wlab:.0f}" height="13" rx="2" fill="{PAPER}"/>')
            self.camada_rot.append(text(cx, ty, p, 10, ACCENT if kind in ("acc", "exc") else (INK if kind == "call" else MUTED)))
        if len(partes) > 1:
            self.y += 13 * (len(partes) - 1)
            # reposiciona a seta para baixo dos rótulos extras
            ultima = self.camada_seta.pop()
            ny = self.y
            self.camada_seta.append(ultima.replace(f'y1="{y}"', f'y1="{ny}"').replace(f'y2="{y}"', f'y2="{ny}"'))
            # desloca rótulos para cima da nova linha
            k = len(partes) * 2
            novos = []
            for item in self.camada_rot[-k:]:
                item = item.replace(f'y="', 'y="', 1)
                novos.append(item)
            self.camada_rot[-k:] = [self._shift(it, ny - y) for it in self.camada_rot[-k:]]
        return self.y

    @staticmethod
    def _shift(el, dy):
        import re
        return re.sub(r' y="([\d.\-]+)"', lambda m: f' y="{float(m.group(1)) + dy:.0f}"', el, count=1)

    def auto(self, a, rotulo, gap=40):
        """mensagem para si mesmo (laço em U à direita)"""
        self.y += gap
        y0, y1 = self.y - 12, self.y + 8
        x = self.x[a] + 4
        self.camada_seta.append(
            f'<path d="M{x} {y0} H{x+36} Q{x+44} {y0} {x+44} {y0+8} V{y1-8} Q{x+44} {y1} {x+36} {y1} H{x+6}" '
            f'fill="none" stroke="{MUTED}" stroke-width="1.2" marker-end="url(#{self.slug}-arrow)"/>')
        self.n += 1
        lab = f"{self.n}  {rotulo}"
        wlab = len(lab) * CHAR_MONO + 12
        self.camada_rot.append(f'<rect x="{x+52}" y="{y0+2}" width="{wlab:.0f}" height="13" rx="2" fill="{PAPER}"/>')
        self.camada_rot.append(text(x + 58, y0 + 12, lab, 10, INK, anchor="start"))
        self.y = y1

    def nota(self, a, b, s):
        """nota curta entre/abaixo de lifelines (callout itálico)"""
        self.y += 28
        cx = (self.x[a] + self.x[b]) / 2
        w = len(s) * 5.6 + 24
        self.camada_rot.append(f'<rect x="{cx - w/2:.0f}" y="{self.y - 14}" width="{w:.0f}" height="20" rx="4" fill="#ffffff" stroke="rgba(45,49,66,0.22)" stroke-width="1"/>')
        self.camada_rot.append(text(cx, self.y, s, 10, MUTED, family="'Instrument Serif', serif", italic=True))

    # ---------- fragmentos ----------
    def frag_inicio(self, op, guarda, a, b):
        self.y += 24
        f = dict(op=op, a=a, b=b, y=self.y, guardas=[(self.y + 32, guarda)], div=[])
        self.y += 32
        return f

    def frag_divisor(self, f, guarda):
        self.y += 20
        f["div"].append(self.y)
        f["guardas"].append((self.y + 20, guarda))
        self.y += 20

    def frag_fim(self, f, extra_dir=0):
        self.y += 20
        x0 = self.x[f["a"]] - self.bw / 2 + 8
        x1 = self.x[f["b"]] + self.bw / 2 - 8 + extra_dir
        y0, h = f["y"], self.y - f["y"]
        tabw = len(f["op"]) * 7 + 20
        c = self.camada_frag
        c.append(f'<rect x="{x0:.0f}" y="{y0}" width="{x1-x0:.0f}" height="{h}" rx="4" fill="rgba(45,49,66,0.02)" stroke="rgba(45,49,66,0.22)" stroke-width="1"/>')
        c.append(f'<rect x="{x0:.0f}" y="{y0}" width="{tabw}" height="16" rx="2" fill="{PAPER}" stroke="rgba(45,49,66,0.22)" stroke-width="1"/>')
        c.append(text(x0 + tabw / 2, y0 + 12, f["op"].upper(), 8, MUTED, ls="0.12em"))
        for gy, g in f["guardas"]:
            gw = len(g) * 6.0 + 8
            self.camada_rot.append(f'<rect x="{x0+8:.0f}" y="{gy-14}" width="{gw:.0f}" height="14" rx="2" fill="{PAPER}"/>')
            self.camada_rot.append(text(x0 + 12, gy - 4, g, 9.5, MUTED, anchor="start", ls="0.02em"))
        for dy in f["div"]:
            c.append(f'<line x1="{x0+8:.0f}" y1="{dy}" x2="{x1-8:.0f}" y2="{dy}" stroke="rgba(45,49,66,0.20)" stroke-width="1" stroke-dasharray="4,3"/>')

    def ativ(self, lid, y0, y1):
        x = self.x[lid]
        self.camada_ativ.append(f'<rect x="{x-4}" y="{y0-4}" width="8" height="{y1-y0+8}" fill="rgba(45,49,66,0.06)" stroke="{MUTED}" stroke-width="0.8"/>')

    # ---------- montagem ----------
    def svg(self, legenda=True):
        fundo = self.y + 40
        altura = fundo + (84 if legenda else 24)
        s = self.slug
        out = [f'<svg viewBox="0 0 {self.w} {altura}" xmlns="http://www.w3.org/2000/svg" width="{self.w}" height="{altura}" role="img" aria-labelledby="{s}-title {s}-desc" style="min-width:{self.w}px">',
               f'<title id="{s}-title">{escape(self.titulo)}</title>',
               f'<desc id="{s}-desc">{escape(self.desc)}</desc>',
               '<defs>',
               f'<pattern id="{s}-dots" width="22" height="22" patternUnits="userSpaceOnUse"><circle cx="1" cy="1" r="0.9" fill="rgba(45,49,66,0.10)"/></pattern>',
               f'<marker id="{s}-arrow" markerWidth="8" markerHeight="6" refX="7" refY="3" orient="auto"><polygon points="0 0, 8 3, 0 6" fill="{MUTED}"/></marker>',
               f'<marker id="{s}-arrow-accent" markerWidth="8" markerHeight="6" refX="7" refY="3" orient="auto"><polygon points="0 0, 8 3, 0 6" fill="{ACCENT}"/></marker>',
               '</defs>',
               f'<rect width="100%" height="100%" fill="{PAPER}"/>',
               f'<rect width="100%" height="100%" fill="url(#{s}-dots)" opacity="0.55"/>']
        for lid, *_ in self.ll:
            x = self.x[lid]
            out.append(f'<line x1="{x}" y1="{self.top}" x2="{x}" y2="{fundo}" stroke="rgba(45,49,66,0.22)" stroke-width="1" stroke-dasharray="3,3"/>')
        out += self.camada_frag + self.camada_ativ + self.camada_seta + self.camada_rot
        # caixas dos participantes (desenhadas depois das setas)
        for lid, nome, sub, tipo in self.ll:
            t = TREAT[tipo]
            x = self.x[lid]
            x0, y0, h = x - self.bw / 2, self.top - 56, 56
            out.append(f'<rect x="{x0:.0f}" y="{y0}" width="{self.bw}" height="{h}" rx="6" fill="{PAPER}"/>')
            out.append(f'<rect x="{x0:.0f}" y="{y0}" width="{self.bw}" height="{h}" rx="6" fill="{t["fill"]}" stroke="{t["stroke"]}" stroke-width="1"/>')
            tw = len(t["tag"]) * 5.2 + 10
            tagcor = ACCENT if tipo == "focal" else SOFT
            out.append(f'<rect x="{x0+8:.0f}" y="{y0+8}" width="{tw:.0f}" height="12" rx="2" fill="transparent" stroke="{tagcor}" stroke-opacity="0.5" stroke-width="0.8"/>')
            out.append(text(x0 + 8 + tw / 2, y0 + 17, t["tag"], 7, tagcor, ls="0.08em"))
            fs = 11 if len(nome) * CHAR_SANS <= self.bw - 12 else 10
            out.append(text(x, y0 + 36, nome, fs, INK, family=SANS, weight="600"))
            out.append(text(x, y0 + 49, sub, 8.5, MUTED))
        if legenda:
            ly = fundo + 24
            out.append(f'<line x1="{self.margem/2}" y1="{ly}" x2="{self.w - self.margem/2}" y2="{ly}" stroke="rgba(45,49,66,0.10)" stroke-width="0.8"/>')
            out.append(text(self.margem / 2, ly + 18, "LEGENDA", 8, MUTED, anchor="start", ls="0.18em"))
            lx, yy = self.margem / 2, ly + 40
            def item(dx, html, rot):
                out.append(html.format(x=lx + dx, y=yy))
                out.append(text(lx + dx + 36, yy + 4, rot, 9, MUTED, family=SANS, anchor="start"))
            item(0, f'<line x1="{lx}" y1="{yy}" x2="{lx+28}" y2="{yy}" stroke="{MUTED}" stroke-width="1.2" marker-end="url(#{s}-arrow)"/>', "chamada")
            item(120, f'<line x1="{lx+120}" y1="{yy}" x2="{lx+148}" y2="{yy}" stroke="{MUTED}" stroke-width="1.2" stroke-dasharray="5,4" marker-end="url(#{s}-arrow)"/>', "retorno")
            item(240, f'<line x1="{lx+240}" y1="{yy}" x2="{lx+268}" y2="{yy}" stroke="{ACCENT}" stroke-width="1.4" marker-end="url(#{s}-arrow-accent)"/>', "resultado principal / exceção (tracejada)")
            item(500, f'<rect x="{lx+500}" y="{yy-8}" width="28" height="16" rx="2" fill="rgba(45,49,66,0.02)" stroke="rgba(45,49,66,0.22)"/>', "fragmento (alt / opt / loop)")
            item(720, f'<rect x="{lx+724}" y="{yy-9}" width="8" height="18" fill="rgba(45,49,66,0.06)" stroke="{MUTED}" stroke-width="0.8"/>', "ativação")
        out.append("</svg>")
        return "\n".join(out)


# ===================== PARTE 1 — resumo antes de fechar =====================
p1 = Painel("uc03-p1", "UC03 · Parte 1 — Resumo antes de fechar",
            "O usuário abre Fechar mês; o ViewModel pede o resumo ao ConsultarResumoUseCase, que soma os lançamentos do mês em ResumoDoPeriodo (RN04).",
            [("U", "Usuário", "ator", "ator"),
             ("Tela", "FecharMesScreen", "ui · Compose", "ui"),
             ("VM", "FecharMesViewModel", "ui · estado", "vm"),
             ("CR", "ConsultarResumoUseCase", "usecase", "uc"),
             ("LR", "LancamentoRepository", "domain.repositorio", "repo"),
             ("Res", "ResumoDoPeriodo", "domain.model", "dom")])
p1.msg("U", "Tela", 'abre "Fechar mês"')
a = p1.msg("Tela", "VM", "carregar(mes)")
b = p1.msg("VM", "CR", "doMes(mes, titular = null)")
c = p1.msg("CR", "LR", "doMes(mes)")
p1.msg("LR", "CR", "lancamentos", "ret")
p1.msg("CR", "Res", "de(lancamentos, null)")
d = p1.msg("Res", "CR", "resumo", "ret")
e = p1.msg("CR", "VM", "resumo  [RN04]", "ret", linhas=["resumo  [RN04]", "receitas · gasto total · terceiros · sobra"])
f = p1.msg("VM", "Tela", "estado (resumo + previstos)", "acc")
p1.ativ("VM", a, f); p1.ativ("CR", b, e); p1.ativ("Res", d - 36, d); p1.ativ("LR", c, c + 36)
p1.msg("Tela", "U", "mostra o resumo", "ret")
fr = p1.frag_inicio("opt", "[há previstos não conferidos (A1)]", "U", "Tela")
p1.msg("U", "Tela", "ajusta valores (UC09) ou segue", gap=28)
p1.frag_fim(fr)

# ===================== PARTE 2 — fechamento em transação =====================
p2 = Painel("uc03-p2", "UC03 · Parte 2 — Fechar o mês numa única transação",
            "FecharMesUseCase fecha a competência atual (RN08), abre a seguinte e grava só os previstos que ainda não existem, tudo dentro de Transacao.",
            [("Tela", "FecharMesScreen", "ui · Compose", "ui"),
             ("VM", "FecharMesViewModel", "ui · estado", "vm"),
             ("UC", "FecharMesUseCase", "usecase", "focal"),
             ("Tx", "Transacao", "domain.repositorio", "repo"),
             ("CRepo", "CompetenciaRepository", "domain.repositorio", "repo"),
             ("At", "atual : Competencia", "domain.model", "dom"),
             ("RRepo", "RecorrenciaRepository", "domain.repositorio", "repo"),
             ("Fab", "FabricaDeLancamentos", "domain.regra", "fab"),
             ("LRepo", "LancamentoRepository", "domain.repositorio", "repo")])
p2.nota("Tela", "VM", "usuário confirma o fechamento")
y_f = p2.msg("Tela", "VM", "fechar()")
y_e = p2.msg("VM", "UC", "executar(mes)")
y_tx = p2.msg("UC", "Tx", "executar { … }")
p2.msg("UC", "CRepo", "buscarOuCriar(mes)")
p2.msg("CRepo", "UC", "atual", "ret")
y_at = p2.msg("UC", "At", "fechar()")
alt = p2.frag_inicio("alt", "[já estava fechada (E1)]", "VM", "At")
p2.msg("At", "UC", "«lança» CompetenciaFechada  [RN08]", "exc", gap=28)
p2.msg("UC", "VM", "CompetenciaFechada", "exc")
p2.msg("VM", "Tela", 'evento "reabrir?"', "ret")
p2.frag_divisor(alt, "[aberta]")
p2.msg("UC", "CRepo", "salvar(atual)", gap=28)
p2.msg("UC", "CRepo", "buscarOuCriar(mes.proximo())")
p2.msg("CRepo", "UC", "seguinte", "ret")
p2.msg("UC", "RRepo", "contasFixasAtivas()")
p2.msg("UC", "RRepo", "receitasAtivas()")
p2.msg("UC", "RRepo", "investimentosComAporte()")
p2.msg("RRepo", "UC", "contas, receitas, investimentos", "ret")
y_fab = p2.msg("UC", "Fab", "previstosDoMes(...)", linhas=["previstosDoMes(seguinte.mesAno,", "contas, receitas, investimentos)"])
p2.msg("Fab", "UC", "previstos  (ver Parte 3)", "ret")
p2.ativ("Fab", y_fab, p2.y)
p2.msg("UC", "LRepo", "doMes(seguinte.mesAno)")
p2.msg("LRepo", "UC", "existentes", "ret")
p2.auto("UC", "semDuplicados(previstos, existentes)  (A3 · por origemId)")
p2.msg("UC", "LRepo", "salvarTodos(novos)", gap=36)
p2.frag_fim(alt, extra_dir=p2.passo * 3)
y_ok = p2.msg("Tx", "UC", "confirma tudo ou desfaz tudo", "ret")
p2.ativ("Tx", y_tx, y_ok)
y_seg = p2.msg("UC", "VM", "seguinte", "ret")
p2.ativ("UC", y_e, y_seg)
y_fim = p2.msg("VM", "Tela", "estado (mês seguinte)", "acc")
p2.ativ("VM", y_f, y_fim)

# ===================== PARTE 3 — geração dos previstos =====================
p3 = Painel("uc03-p3", "UC03 · Parte 3 — Gerar os previstos do mês seguinte",
            "FabricaDeLancamentos pede a cada recorrência ativa o seu lançamento PREVISTO: contas fixas (RN06), salário (RN07) e aportes planejados (RN11).",
            [("UC", "FecharMesUseCase", "usecase", "focal"),
             ("Fab", "FabricaDeLancamentos", "domain.regra · Factory", "fab"),
             ("CF", "ContaFixa", "domain.model · Creator", "dom"),
             ("RR", "ReceitaRecorrente", "domain.model · Creator", "dom"),
             ("CI", "ContaInvestimento", "domain.model · Creator", "dom")],
            largura_caixa=148, passo=184)
y0 = p3.msg("UC", "Fab", "previstosDoMes(mes, contas, receitas, investimentos)")
l1 = p3.frag_inicio("loop", "[cada conta fixa ativa]", "Fab", "CF")
p3.msg("Fab", "CF", "gerarPrevisto(mes)", gap=28)
p3.msg("CF", "Fab", "gasto PREVISTO  [RN06]", "ret", linhas=["gasto PREVISTO  [RN06]", "dia inexistente → último dia"])
p3.frag_fim(l1)
l2 = p3.frag_inicio("loop", "[cada receita recorrente]", "Fab", "RR")
p3.msg("Fab", "RR", "gerarPrevista(mes)", gap=28)
p3.msg("RR", "Fab", "receita PREVISTA  [RN07]", "ret")
p3.frag_fim(l2)
l3 = p3.frag_inicio("loop", "[cada corretora com aporte planejado]", "Fab", "CI")
p3.msg("Fab", "CI", "gerarAportePrevisto(mes)", gap=28)
p3.msg("CI", "Fab", "aporte PREVISTO  [RN11]", "ret")
p3.frag_fim(l3)
y1 = p3.msg("Fab", "UC", "previstos (cada um com origemId)", "acc")
p3.ativ("Fab", y0, y1)

CSS = f"""
*,*::before,*::after{{box-sizing:border-box;margin:0;padding:0}}
:root{{--paper:{PAPER};--ink:{INK};--muted:{MUTED};--accent:{ACCENT}}}
body{{font-family:'Geist',system-ui,sans-serif;background:var(--paper);color:var(--ink);padding:3rem 2rem}}
.frame{{max-width:1560px;margin:0 auto}}
.eyebrow{{font-family:'Geist Mono',monospace;font-size:.66rem;font-weight:500;letter-spacing:.18em;text-transform:uppercase;color:var(--muted);margin-bottom:.5rem}}
h1{{font-family:'Instrument Serif',serif;font-size:2rem;font-weight:400;letter-spacing:-.02em;margin-bottom:.4rem}}
.lead{{color:var(--muted);max-width:70ch;margin-bottom:2rem;line-height:1.5}}
h2{{font-family:'Instrument Serif',serif;font-weight:400;font-size:1.35rem;margin:2.5rem 0 .25rem}}
h2 span{{font-family:'Geist Mono',monospace;font-size:.7rem;letter-spacing:.14em;color:var(--accent);margin-right:.6rem}}
p.cap{{color:var(--muted);font-size:.9rem;margin-bottom:.9rem}}
.wrap{{overflow-x:auto;border:1px solid rgba(45,49,66,.12);border-radius:6px;background:var(--paper)}}
svg{{display:block}}
"""


# ===================== UC02 — PARTE 1: prévia =====================
q1 = Painel("uc02-p1", "UC02 · Parte 1 — Prévia das parcelas",
            "O ViewModel pede a prévia; a Factory valida e cria a CompraParcelada, que usa a política do cartão (RN01) e divide o valor em centavos (RN02).",
            [("Tela", "RegistrarGastoScreen", "ui · Compose", "ui"),
             ("VM", "RegistrarGastoViewModel", "ui · estado", "vm"),
             ("UC", "RegistrarCompraParceladaUseCase", "usecase", "focal"),
             ("Fab", "FabricaDeLancamentos", "domain.regra · Factory", "fab"),
             ("CP", "compra : CompraParcelada", "domain.model · Creator", "dom"),
             ("Pol", "politica do cartão", "Strategy · RN01", "dom"),
             ("Par", "Parcela", "domain.model", "dom")],
            largura_caixa=200, passo=212)
q1.nota("Tela", "VM", "usuário informa cartão, valor, nº de parcelas, data")
a1 = q1.msg("Tela", "VM", "previaParcelas()")
a2 = q1.msg("VM", "UC", "previa(dados)")
a3 = q1.msg("UC", "Fab", "compraParcelada(dados)")
alt = q1.frag_inicio("alt", "[valor <= 0  ou  parcelas fora de 2..48]", "VM", "Fab")
q1.msg("Fab", "UC", "«lança» ValorInvalido | ParcelasForaDoLimite", "exc", gap=28)
q1.msg("UC", "VM", "exceção da regra", "exc")
q1.msg("VM", "Tela", "estado com erro", "ret")
q1.frag_divisor(alt, "[dados válidos]")
q1.msg("Fab", "CP", "«create»", gap=28)
a4 = q1.msg("Fab", "UC", "compra", "ret")
q1.ativ("Fab", a3, a4)
a5 = q1.msg("UC", "CP", "gerarParcelas()")
q1.msg("CP", "Pol", "cartao.politicaDeCompetencia() · competenciaDe(data)", linhas=["cartao.politicaDeCompetencia()", "competenciaDe(dataCompra)"])
q1.msg("Pol", "CP", "mes1  [RN01]", "ret")
q1.auto("CP", "valorTotal.dividirEm(N)  — resto na 1ª [RN02]")
lp = q1.frag_inicio("loop", "[k = 1..N]", "CP", "Par")
q1.msg("CP", "Par", "«create» k/N, valor, mes1 + k − 1", gap=28)
q1.frag_fim(lp)
a6 = q1.msg("CP", "UC", "parcelas", "ret")
q1.ativ("CP", a5, a6)
a7 = q1.msg("UC", "VM", "parcelas", "ret")
q1.ativ("UC", a2, a7)
a8 = q1.msg("VM", "Tela", "estado: prévia  k/N – valor – fatura", "acc")
q1.ativ("VM", a1, a8)
q1.frag_fim(alt, extra_dir=q1.passo * 3)

# ===================== UC02 — PARTE 2: confirmação =====================
q2 = Painel("uc02-p2", "UC02 · Parte 2 — Confirmar e gravar as parcelas",
            "Na confirmação o use case repete a criação, garante que a competência da 1ª parcela está aberta (RN08) e grava todas as parcelas.",
            [("Tela", "RegistrarGastoScreen", "ui · Compose", "ui"),
             ("VM", "RegistrarGastoViewModel", "ui · estado", "vm"),
             ("UC", "RegistrarCompraParceladaUseCase", "usecase", "focal"),
             ("CRepo", "CompetenciaRepository", "domain.repositorio", "repo"),
             ("Comp", "competencia : Competencia", "domain.model", "dom"),
             ("LRepo", "LancamentoRepository", "domain.repositorio", "repo")],
            largura_caixa=200, passo=212)
q2.nota("Tela", "VM", "usuário confirma a prévia")
b1 = q2.msg("Tela", "VM", "confirmar()")
b2 = q2.msg("VM", "UC", "executar(dados)")
q2.nota("UC", "CRepo", "repete a Parte 1: validar, criar, gerar parcelas")
q2.msg("UC", "CRepo", "buscarOuCriar(mes1)")
q2.msg("CRepo", "UC", "competencia", "ret")
q2.msg("UC", "Comp", "garantirAberta()")
alt2 = q2.frag_inicio("alt", "[competência fechada]", "VM", "Comp")
q2.msg("Comp", "UC", "«lança» CompetenciaFechada [RN08]", "exc", gap=28)
q2.msg("UC", "VM", "CompetenciaFechada", "exc")
q2.msg("VM", "Tela", 'evento "reabrir?"', "ret")
q2.frag_divisor(alt2, "[competência aberta]")
q2.msg("UC", "LRepo", "salvarTodos(parcelas)", gap=28)
b3 = q2.msg("UC", "VM", "compra", "ret")
q2.ativ("UC", b2, b3)
b4 = q2.msg("VM", "Tela", "estado: fatura atualizada", "acc")
q2.ativ("VM", b1, b4)
q2.frag_fim(alt2, extra_dir=q2.passo)

CSS = CSS  # reaproveita

def pagina(titulo, eyebrow, h1, lead, partes):
    corpo = "".join(
        f'<h2><span>PARTE {i}</span>{escape(nome)}</h2><p class="cap">{escape(cap)}</p><div class="wrap">{p.svg()}</div>'
        for i, (nome, cap, p) in enumerate(partes, 1))
    return f"""<!DOCTYPE html>
<html lang="pt-BR"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>{escape(titulo)}</title>
<link href="https://fonts.googleapis.com/css2?family=Instrument+Serif:ital@0;1&family=Geist:wght@400;500;600&family=Geist+Mono:wght@400;500;600&display=swap" rel="stylesheet">
<style>{CSS}</style></head>
<body><div class="frame">
<p class="eyebrow">{escape(eyebrow)}</p>
<h1>{escape(h1)}</h1>
<p class="lead">{lead}</p>
{corpo}
</div></body></html>"""


def main(pasta):
    import os
    uc02 = pagina("UC02 · Compra parcelada · Sequência", "Diagrama de sequência · GastoCerto · UC02",
                  "Registrar compra parcelada",
                  "Versão de apresentação, em duas partes: a prévia e a confirmação. A fonte formal está em <code>docs/uml/sequencia-uc02-*.puml</code>.",
                  [("Prévia das parcelas", "RN01 · competência pelo fechamento do cartão · RN02 · 2 a 48 parcelas, resto na 1ª", q1),
                   ("Confirmar e gravar", "RN08 · competência FECHADA não aceita lançamentos", q2)])
    uc03 = pagina("UC03 · Fechar o mês · Sequência", "Diagrama de sequência · GastoCerto · UC03",
                  "Fechar o mês e abrir o seguinte",
                  "Versão de apresentação, em três partes encadeadas. A Parte 3 detalha a chamada <code>previstosDoMes</code> da Parte 2. A fonte formal está em <code>docs/uml/sequencia-uc03-*.puml</code>.",
                  [("Resumo antes de fechar", "RN04 · sobra = receitas − (gasto total − gastos de terceiros)", p1),
                   ("Fechamento numa única transação", "RN08 · competência FECHADA não aceita lançamentos · A3 · previstos não duplicados (mesmo origemId)", p2),
                   ("Geração dos previstos (Factory + Creator)", "RN06 contas fixas · RN07 salário · RN11 aportes planejados", p3)])
    for nome, html in (("sequencia-uc02.html", uc02), ("sequencia-uc03.html", uc03)):
        open(os.path.join(pasta, nome), "w", encoding="utf-8").write(html)
    if os.environ.get("VERIFICAR_PAINEIS"):
        for p in (q1, q2, p1, p2, p3):
            open(os.path.join(os.environ["VERIFICAR_PAINEIS"], f"check-{p.slug}.html"), "w", encoding="utf-8").write(
                f"<!DOCTYPE html><html lang='pt-BR'><head><meta charset='UTF-8'><title>{p.slug}</title></head><body>{p.svg()}</body></html>")
    print("ok", [p.w for p in (q1, q2, p1, p2, p3)])


if __name__ == "__main__":
    main(sys.argv[1])
