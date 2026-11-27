"""Generates the PRESENTATION versions of the UC02 and UC03 sequence diagrams
in the diagram-design skill style (default tokens, HTML + inline SVG).

The source of truth for the design is still docs/uml/*.puml (PlantUML).
These HTML files are an editorial re-reading for the slides: when a .puml changes,
update this script and run it again. Slide text stays in Portuguese (it is presentation
content); class and method names are the English ones from the code.

Usage: python3 generate_sequence_diagrams.py <output-folder>
Output: sequencia-uc02.html and sequencia-uc03.html
"""
import sys
from html import escape

PAPER, PAPER2, INK, MUTED, SOFT = "#f5f5f5", "#ececec", "#2d3142", "#4f5d75", "#5a6580"
ACCENT, ACCENT_TINT, LINK = "#bf4520", "rgba(191,69,32,0.08)", "#2e5aa8"
SANS, MONO = "'Geist', system-ui, sans-serif", "'Geist Mono', ui-monospace, monospace"

# treatment per layer (style-guide: Node type -> treatment)
TREAT = {
    "actor":  dict(fill="rgba(79,93,117,0.10)", stroke=SOFT, tag="ATOR"),
    "ui":     dict(fill="rgba(79,93,117,0.10)", stroke=SOFT, tag="UI"),
    "vm":     dict(fill="rgba(79,93,117,0.10)", stroke=SOFT, tag="VIEWMODEL"),
    "uc":     dict(fill="#ffffff", stroke=INK, tag="USE CASE"),
    "focal":  dict(fill=ACCENT_TINT, stroke=ACCENT, tag="USE CASE"),
    "dom":    dict(fill="#ffffff", stroke=INK, tag="DOMÍNIO"),
    "fab":    dict(fill="#ffffff", stroke=INK, tag="FACTORY"),
    "repo":   dict(fill="rgba(45,49,66,0.05)", stroke=MUTED, tag="REPOSITÓRIO"),
}

CHAR_MONO = 6.0   # approx. width of a Geist Mono character at 10px
CHAR_SANS = 6.4   # Geist 600 at 11px


def text(x, y, s, size=10, fill=INK, family=MONO, anchor="middle", weight=None, ls=None, italic=False):
    w = f' font-weight="{weight}"' if weight else ""
    l = f' letter-spacing="{ls}"' if ls else ""
    it = ' font-style="italic"' if italic else ""
    return (f'<text x="{x}" y="{y}" fill="{fill}" font-size="{size}" font-family="{family}"'
            f' text-anchor="{anchor}"{w}{l}{it}>{escape(s)}</text>')


class Panel:
    def __init__(self, slug, title, desc, lifelines, box_width=136, step=152, margin=72):
        self.slug, self.title, self.desc = slug, title, desc
        self.ll = lifelines            # list of (id, name, sub, kind)
        self.bw, self.step, self.margin = box_width, step, margin
        self.x = {lid: margin + box_width / 2 + i * step for i, (lid, *_ ) in enumerate(lifelines)}
        self.w = int(margin * 2 + box_width + (len(lifelines) - 1) * step)
        self.top = 120               # bottom of the boxes
        self.y = self.top + 40       # time cursor
        self.arrow_layer, self.label_layer, self.fragment_layer, self.activation_layer = [], [], [], []
        self.n = 0

    # ---------- messages ----------
    def message(self, a, b, label, kind="call", numbered=True, lines=None, gap=36):
        """kind: call | ret | acc (headline) | exc (exception)"""
        self.y += gap
        y = self.y
        xa, xb = self.x[a], self.x[b]
        direction = 1 if xb > xa else -1
        x1, x2 = xa + 4 * direction, xb - 6 * direction
        color = {"call": MUTED, "ret": MUTED, "acc": ACCENT, "exc": ACCENT}[kind]
        marker = {"call": "arrow", "ret": "arrow", "acc": "arrow-accent", "exc": "arrow-accent"}[kind]
        dash = ' stroke-dasharray="5,4"' if kind in ("ret", "exc") else ""
        sw = "1.4" if kind == "acc" else "1.2"
        self.arrow_layer.append(f'<line x1="{x1}" y1="{y}" x2="{x2}" y2="{y}" stroke="{color}" stroke-width="{sw}"{dash} marker-end="url(#{self.slug}-{marker})"/>')
        if numbered:
            self.n += 1
            label = f"{self.n}  {label}"
        parts = lines or [label]
        if lines and numbered:
            parts = [f"{self.n}  {lines[0]}"] + lines[1:]
        cx = (xa + xb) / 2
        for i, p in enumerate(reversed(parts)):
            ty = y - 8 - i * 13
            label_width = len(p) * CHAR_MONO + 12
            self.label_layer.append(f'<rect x="{cx - label_width/2:.0f}" y="{ty - 10}" width="{label_width:.0f}" height="13" rx="2" fill="{PAPER}"/>')
            self.label_layer.append(text(cx, ty, p, 10, ACCENT if kind in ("acc", "exc") else (INK if kind == "call" else MUTED)))
        if len(parts) > 1:
            self.y += 13 * (len(parts) - 1)
            # moves the arrow below the extra labels
            last = self.arrow_layer.pop()
            ny = self.y
            self.arrow_layer.append(last.replace(f'y1="{y}"', f'y1="{ny}"').replace(f'y2="{y}"', f'y2="{ny}"'))
            # shifts labels above the new line
            k = len(parts) * 2
            new_items = []
            for item in self.label_layer[-k:]:
                item = item.replace(f'y="', 'y="', 1)
                new_items.append(item)
            self.label_layer[-k:] = [self._shift(it, ny - y) for it in self.label_layer[-k:]]
        return self.y

    @staticmethod
    def _shift(el, dy):
        import re
        return re.sub(r' y="([\d.\-]+)"', lambda m: f' y="{float(m.group(1)) + dy:.0f}"', el, count=1)

    def self_call(self, a, label, gap=40):
        """message to itself (U-shaped loop on the right)"""
        self.y += gap
        y0, y1 = self.y - 12, self.y + 8
        x = self.x[a] + 4
        self.arrow_layer.append(
            f'<path d="M{x} {y0} H{x+36} Q{x+44} {y0} {x+44} {y0+8} V{y1-8} Q{x+44} {y1} {x+36} {y1} H{x+6}" '
            f'fill="none" stroke="{MUTED}" stroke-width="1.2" marker-end="url(#{self.slug}-arrow)"/>')
        self.n += 1
        lab = f"{self.n}  {label}"
        label_width = len(lab) * CHAR_MONO + 12
        self.label_layer.append(f'<rect x="{x+52}" y="{y0+2}" width="{label_width:.0f}" height="13" rx="2" fill="{PAPER}"/>')
        self.label_layer.append(text(x + 58, y0 + 12, lab, 10, INK, anchor="start"))
        self.y = y1

    def note(self, a, b, s):
        """short note between/below lifelines (italic callout)"""
        self.y += 28
        cx = (self.x[a] + self.x[b]) / 2
        w = len(s) * 5.6 + 24
        self.label_layer.append(f'<rect x="{cx - w/2:.0f}" y="{self.y - 14}" width="{w:.0f}" height="20" rx="4" fill="#ffffff" stroke="rgba(45,49,66,0.22)" stroke-width="1"/>')
        self.label_layer.append(text(cx, self.y, s, 10, MUTED, family="'Instrument Serif', serif", italic=True))

    # ---------- fragments ----------
    def fragment_start(self, op, guard, a, b):
        self.y += 24
        f = dict(op=op, a=a, b=b, y=self.y, guards=[(self.y + 32, guard)], div=[])
        self.y += 32
        return f

    def fragment_divider(self, f, guard):
        self.y += 20
        f["div"].append(self.y)
        f["guards"].append((self.y + 20, guard))
        self.y += 20

    def fragment_end(self, f, extra_right=0):
        self.y += 20
        x0 = self.x[f["a"]] - self.bw / 2 + 8
        x1 = self.x[f["b"]] + self.bw / 2 - 8 + extra_right
        y0, h = f["y"], self.y - f["y"]
        tabw = len(f["op"]) * 7 + 20
        c = self.fragment_layer
        c.append(f'<rect x="{x0:.0f}" y="{y0}" width="{x1-x0:.0f}" height="{h}" rx="4" fill="rgba(45,49,66,0.02)" stroke="rgba(45,49,66,0.22)" stroke-width="1"/>')
        c.append(f'<rect x="{x0:.0f}" y="{y0}" width="{tabw}" height="16" rx="2" fill="{PAPER}" stroke="rgba(45,49,66,0.22)" stroke-width="1"/>')
        c.append(text(x0 + tabw / 2, y0 + 12, f["op"].upper(), 8, MUTED, ls="0.12em"))
        for gy, g in f["guards"]:
            gw = len(g) * 6.0 + 8
            self.label_layer.append(f'<rect x="{x0+8:.0f}" y="{gy-14}" width="{gw:.0f}" height="14" rx="2" fill="{PAPER}"/>')
            self.label_layer.append(text(x0 + 12, gy - 4, g, 9.5, MUTED, anchor="start", ls="0.02em"))
        for dy in f["div"]:
            c.append(f'<line x1="{x0+8:.0f}" y1="{dy}" x2="{x1-8:.0f}" y2="{dy}" stroke="rgba(45,49,66,0.20)" stroke-width="1" stroke-dasharray="4,3"/>')

    def activation(self, lid, y0, y1):
        x = self.x[lid]
        self.activation_layer.append(f'<rect x="{x-4}" y="{y0-4}" width="8" height="{y1-y0+8}" fill="rgba(45,49,66,0.06)" stroke="{MUTED}" stroke-width="0.8"/>')

    # ---------- assembly ----------
    def svg(self, legend=True):
        bottom = self.y + 40
        height = bottom + (84 if legend else 24)
        s = self.slug
        out = [f'<svg viewBox="0 0 {self.w} {height}" xmlns="http://www.w3.org/2000/svg" width="{self.w}" height="{height}" role="img" aria-labelledby="{s}-title {s}-desc" style="min-width:{self.w}px">',
               f'<title id="{s}-title">{escape(self.title)}</title>',
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
            out.append(f'<line x1="{x}" y1="{self.top}" x2="{x}" y2="{bottom}" stroke="rgba(45,49,66,0.22)" stroke-width="1" stroke-dasharray="3,3"/>')
        out += self.fragment_layer + self.activation_layer + self.arrow_layer + self.label_layer
        # participant boxes (drawn after the arrows)
        for lid, name, sub, kind in self.ll:
            t = TREAT[kind]
            x = self.x[lid]
            x0, y0, h = x - self.bw / 2, self.top - 56, 56
            out.append(f'<rect x="{x0:.0f}" y="{y0}" width="{self.bw}" height="{h}" rx="6" fill="{PAPER}"/>')
            out.append(f'<rect x="{x0:.0f}" y="{y0}" width="{self.bw}" height="{h}" rx="6" fill="{t["fill"]}" stroke="{t["stroke"]}" stroke-width="1"/>')
            tw = len(t["tag"]) * 5.2 + 10
            tag_color = ACCENT if kind == "focal" else SOFT
            out.append(f'<rect x="{x0+8:.0f}" y="{y0+8}" width="{tw:.0f}" height="12" rx="2" fill="transparent" stroke="{tag_color}" stroke-opacity="0.5" stroke-width="0.8"/>')
            out.append(text(x0 + 8 + tw / 2, y0 + 17, t["tag"], 7, tag_color, ls="0.08em"))
            fs = 11 if len(name) * CHAR_SANS <= self.bw - 12 else 10
            out.append(text(x, y0 + 36, name, fs, INK, family=SANS, weight="600"))
            out.append(text(x, y0 + 49, sub, 8.5, MUTED))
        if legend:
            ly = bottom + 24
            out.append(f'<line x1="{self.margin/2}" y1="{ly}" x2="{self.w - self.margin/2}" y2="{ly}" stroke="rgba(45,49,66,0.10)" stroke-width="0.8"/>')
            out.append(text(self.margin / 2, ly + 18, "LEGENDA", 8, MUTED, anchor="start", ls="0.18em"))
            lx, yy = self.margin / 2, ly + 40
            def item(dx, html, text_label):
                out.append(html.format(x=lx + dx, y=yy))
                out.append(text(lx + dx + 36, yy + 4, text_label, 9, MUTED, family=SANS, anchor="start"))
            item(0, f'<line x1="{lx}" y1="{yy}" x2="{lx+28}" y2="{yy}" stroke="{MUTED}" stroke-width="1.2" marker-end="url(#{s}-arrow)"/>', "chamada")
            item(120, f'<line x1="{lx+120}" y1="{yy}" x2="{lx+148}" y2="{yy}" stroke="{MUTED}" stroke-width="1.2" stroke-dasharray="5,4" marker-end="url(#{s}-arrow)"/>', "retorno")
            item(240, f'<line x1="{lx+240}" y1="{yy}" x2="{lx+268}" y2="{yy}" stroke="{ACCENT}" stroke-width="1.4" marker-end="url(#{s}-arrow-accent)"/>', "resultado principal / exceção (tracejada)")
            item(500, f'<rect x="{lx+500}" y="{yy-8}" width="28" height="16" rx="2" fill="rgba(45,49,66,0.02)" stroke="rgba(45,49,66,0.22)"/>', "fragmento (alt / opt / loop)")
            item(720, f'<rect x="{lx+724}" y="{yy-9}" width="8" height="18" fill="rgba(45,49,66,0.06)" stroke="{MUTED}" stroke-width="0.8"/>', "ativação")
        out.append("</svg>")
        return "\n".join(out)


# ===================== UC03 — PART 1: summary before closing =====================
p1 = Panel("uc03-p1", "UC03 · Parte 1 — Resumo antes de fechar",
            "O usuário abre Fechar mês; o ViewModel pede o resumo ao GetSummaryUseCase, que soma os lançamentos do mês em PeriodSummary (RN04).",
            [("U", "Usuário", "ator", "actor"),
             ("Tela", "CloseMonthScreen", "ui · Compose", "ui"),
             ("VM", "CloseMonthViewModel", "ui · estado", "vm"),
             ("CR", "GetSummaryUseCase", "usecase", "uc"),
             ("LR", "EntryRepository", "domain.repository", "repo"),
             ("Res", "PeriodSummary", "domain.model", "dom")])
p1.message("U", "Tela", 'abre "Fechar mês"')
a = p1.message("Tela", "VM", "load(month)")
b = p1.message("VM", "CR", "ofMonth(month, holder = null)")
c = p1.message("CR", "LR", "ofMonth(month)")
p1.message("LR", "CR", "entries", "ret")
p1.message("CR", "Res", "of(entries, null)")
d = p1.message("Res", "CR", "summary", "ret")
e = p1.message("CR", "VM", "summary  [RN04]", "ret", lines=["summary  [RN04]", "income · totalExpenses · thirdPartyExpenses · surplus()"])
f = p1.message("VM", "Tela", "uiState (resumo + previstos)", "acc")
p1.activation("VM", a, f); p1.activation("CR", b, e); p1.activation("Res", d - 36, d); p1.activation("LR", c, c + 36)
p1.message("Tela", "U", "mostra o resumo", "ret")
frag = p1.fragment_start("opt", "[há previstos não conferidos (A1)]", "U", "Tela")
p1.message("U", "Tela", "ajusta valores (UC09) ou segue", gap=28)
p1.fragment_end(frag)

# ===================== UC03 — PART 2: closing in one transaction =====================
p2 = Panel("uc03-p2", "UC03 · Parte 2 — Fechar o mês numa única transação",
            "CloseMonthUseCase fecha a competência atual (RN08), abre a seguinte e grava só os previstos que ainda não existem, tudo dentro de Transaction.",
            [("Tela", "CloseMonthScreen", "ui · Compose", "ui"),
             ("VM", "CloseMonthViewModel", "ui · estado", "vm"),
             ("UC", "CloseMonthUseCase", "usecase", "focal"),
             ("Tx", "Transaction", "domain.repository", "repo"),
             ("CRepo", "AccountingMonthRepository", "domain.repository", "repo"),
             ("At", "current : AccountingMonth", "domain.model", "dom"),
             ("RRepo", "RecurrenceRepository", "domain.repository", "repo"),
             ("Fab", "EntryFactory", "domain.rule", "fab"),
             ("LRepo", "EntryRepository", "domain.repository", "repo")])
p2.note("Tela", "VM", "usuário confirma o fechamento")
y_f = p2.message("Tela", "VM", "close()")
y_e = p2.message("VM", "UC", "execute(month)")
y_tx = p2.message("UC", "Tx", "execute { … }")
p2.message("UC", "CRepo", "findOrCreate(month)")
p2.message("CRepo", "UC", "current", "ret")
y_at = p2.message("UC", "At", "close()")
alt = p2.fragment_start("alt", "[já estava fechada (E1)]", "VM", "At")
p2.message("At", "UC", "«lança» AccountingMonthClosed  [RN08]", "exc", gap=28)
p2.message("UC", "VM", "AccountingMonthClosed", "exc")
p2.message("VM", "Tela", 'evento "reabrir?"', "ret")
p2.fragment_divider(alt, "[aberta]")
p2.message("UC", "CRepo", "save(current)", gap=28)
p2.message("UC", "CRepo", "findOrCreate(month.next())")
p2.message("CRepo", "UC", "following", "ret")
p2.message("UC", "RRepo", "activeRecurringBills()")
p2.message("UC", "RRepo", "activeRecurringIncomes()")
p2.message("UC", "RRepo", "investmentsWithContribution()")
p2.message("RRepo", "UC", "bills, incomes, investments", "ret")
y_fab = p2.message("UC", "Fab", "expectedForMonth(...)", lines=["expectedForMonth(following.monthYear,", "bills, incomes, investments)"])
p2.message("Fab", "UC", "expected  (ver Parte 3)", "ret")
p2.activation("Fab", y_fab, p2.y)
p2.message("UC", "LRepo", "ofMonth(following.monthYear)")
p2.message("LRepo", "UC", "existing", "ret")
p2.self_call("UC", "withoutDuplicates(expected, existing)  (A3 · por originId)")
p2.message("UC", "LRepo", "saveAll(newEntries)", gap=36)
p2.fragment_end(alt, extra_right=p2.step * 3)
y_ok = p2.message("Tx", "UC", "confirma tudo ou desfaz tudo", "ret")
p2.activation("Tx", y_tx, y_ok)
y_seg = p2.message("UC", "VM", "following", "ret")
p2.activation("UC", y_e, y_seg)
y_fim = p2.message("VM", "Tela", "uiState (mês seguinte)", "acc")
p2.activation("VM", y_f, y_fim)

# ===================== UC03 — PART 3: generating the expected entries =====================
p3 = Panel("uc03-p3", "UC03 · Parte 3 — Gerar os previstos do mês seguinte",
            "EntryFactory pede a cada recorrência ativa o seu lançamento EXPECTED: contas fixas (RN06), salário (RN07) e aportes planejados (RN11).",
            [("UC", "CloseMonthUseCase", "usecase", "focal"),
             ("Fab", "EntryFactory", "domain.rule · Factory", "fab"),
             ("CF", "RecurringBill", "domain.model · Creator", "dom"),
             ("RR", "RecurringIncome", "domain.model · Creator", "dom"),
             ("CI", "InvestmentAccount", "domain.model · Creator", "dom")],
            box_width=148, step=184)
y0 = p3.message("UC", "Fab", "expectedForMonth(month, bills, incomes, investments)")
l1 = p3.fragment_start("loop", "[cada conta fixa ativa]", "Fab", "CF")
p3.message("Fab", "CF", "generateExpected(month)", gap=28)
p3.message("CF", "Fab", "gasto EXPECTED  [RN06]", "ret", lines=["gasto EXPECTED  [RN06]", "dia inexistente → último dia"])
p3.fragment_end(l1)
l2 = p3.fragment_start("loop", "[cada receita recorrente]", "Fab", "RR")
p3.message("Fab", "RR", "generateExpected(month)", gap=28)
p3.message("RR", "Fab", "receita EXPECTED  [RN07]", "ret")
p3.fragment_end(l2)
l3 = p3.fragment_start("loop", "[cada corretora com aporte planejado]", "Fab", "CI")
p3.message("Fab", "CI", "generateExpectedContribution(month)", gap=28)
p3.message("CI", "Fab", "aporte EXPECTED  [RN11]", "ret")
p3.fragment_end(l3)
y1 = p3.message("Fab", "UC", "expected (cada um com originId)", "acc")
p3.activation("Fab", y0, y1)

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


# ===================== UC02 — PART 1: preview =====================
q1 = Panel("uc02-p1", "UC02 · Parte 1 — Prévia das parcelas",
            "O ViewModel pede a prévia; a Factory valida e cria a InstallmentPurchase, que usa a política do cartão (RN01) e divide o valor em centavos (RN02).",
            [("Tela", "RegisterExpenseScreen", "ui · Compose", "ui"),
             ("VM", "RegisterExpenseViewModel", "ui · estado", "vm"),
             ("UC", "RegisterInstallmentPurchaseUseCase", "usecase", "focal"),
             ("Fab", "EntryFactory", "domain.rule · Factory", "fab"),
             ("CP", "purchase : InstallmentPurchase", "domain.model · Creator", "dom"),
             ("Pol", "política do cartão", "Strategy · RN01", "dom"),
             ("Par", "Installment", "domain.model", "dom")],
            box_width=200, step=212)
q1.note("Tela", "VM", "usuário informa cartão, valor, nº de parcelas, data")
a1 = q1.message("Tela", "VM", "previewInstallments()")
a2 = q1.message("VM", "UC", "preview(input)")
a3 = q1.message("UC", "Fab", "installmentPurchase(input)")
alt = q1.fragment_start("alt", "[valor <= 0  ou  parcelas fora de 2..48]", "VM", "Fab")
q1.message("Fab", "UC", "«lança» InvalidAmount | InstallmentsOutOfRange", "exc", gap=28)
q1.message("UC", "VM", "exceção da regra", "exc")
q1.message("VM", "Tela", "uiState com erro", "ret")
q1.fragment_divider(alt, "[dados válidos]")
q1.message("Fab", "CP", "«create»", gap=28)
a4 = q1.message("Fab", "UC", "purchase", "ret")
q1.activation("Fab", a3, a4)
a5 = q1.message("UC", "CP", "generateInstallments()")
q1.message("CP", "Pol", "card.accountingMonthPolicy() · accountingMonthOf(date)", lines=["card.accountingMonthPolicy()", "accountingMonthOf(purchaseDate)"])
q1.message("Pol", "CP", "month1  [RN01]", "ret")
q1.self_call("CP", "totalAmount.splitInto(N)  — resto na 1ª [RN02]")
loop_frag = q1.fragment_start("loop", "[k = 1..N]", "CP", "Par")
q1.message("CP", "Par", "«create» k/N, amount, month1 + k − 1", gap=28)
q1.fragment_end(loop_frag)
a6 = q1.message("CP", "UC", "installments", "ret")
q1.activation("CP", a5, a6)
a7 = q1.message("UC", "VM", "installments", "ret")
q1.activation("UC", a2, a7)
a8 = q1.message("VM", "Tela", "uiState: prévia  k/N – valor – fatura", "acc")
q1.activation("VM", a1, a8)
q1.fragment_end(alt, extra_right=q1.step * 3)

# ===================== UC02 — PART 2: confirmation =====================
q2 = Panel("uc02-p2", "UC02 · Parte 2 — Confirmar e gravar as parcelas",
            "Na confirmação o use case repete a criação, garante que a competência da 1ª parcela está aberta (RN08) e grava todas as parcelas.",
            [("Tela", "RegisterExpenseScreen", "ui · Compose", "ui"),
             ("VM", "RegisterExpenseViewModel", "ui · estado", "vm"),
             ("UC", "RegisterInstallmentPurchaseUseCase", "usecase", "focal"),
             ("CRepo", "AccountingMonthRepository", "domain.repository", "repo"),
             ("Comp", "month : AccountingMonth", "domain.model", "dom"),
             ("LRepo", "EntryRepository", "domain.repository", "repo")],
            box_width=200, step=212)
q2.note("Tela", "VM", "usuário confirma a prévia")
b1 = q2.message("Tela", "VM", "confirm()")
b2 = q2.message("VM", "UC", "execute(input)")
q2.note("UC", "CRepo", "repete a Parte 1: validar, criar, gerar parcelas")
q2.message("UC", "CRepo", "findOrCreate(month1)")
q2.message("CRepo", "UC", "month", "ret")
q2.message("UC", "Comp", "ensureOpen()")
alt2 = q2.fragment_start("alt", "[competência fechada]", "VM", "Comp")
q2.message("Comp", "UC", "«lança» AccountingMonthClosed [RN08]", "exc", gap=28)
q2.message("UC", "VM", "AccountingMonthClosed", "exc")
q2.message("VM", "Tela", 'evento "reabrir?"', "ret")
q2.fragment_divider(alt2, "[competência aberta]")
q2.message("UC", "LRepo", "saveAll(installments)", gap=28)
b3 = q2.message("UC", "VM", "purchase", "ret")
q2.activation("UC", b2, b3)
b4 = q2.message("VM", "Tela", "uiState: fatura atualizada", "acc")
q2.activation("VM", b1, b4)
q2.fragment_end(alt2, extra_right=q2.step)

CSS = CSS  # reused

def page(title, eyebrow, h1, lead, parts):
    body = "".join(
        f'<h2><span>PARTE {i}</span>{escape(name)}</h2><p class="cap">{escape(caption)}</p><div class="wrap">{p.svg()}</div>'
        for i, (name, caption, p) in enumerate(parts, 1))
    return f"""<!DOCTYPE html>
<html lang="pt-BR"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>{escape(title)}</title>
<link href="https://fonts.googleapis.com/css2?family=Instrument+Serif:ital@0;1&family=Geist:wght@400;500;600&family=Geist+Mono:wght@400;500;600&display=swap" rel="stylesheet">
<style>{CSS}</style></head>
<body><div class="frame">
<p class="eyebrow">{escape(eyebrow)}</p>
<h1>{escape(h1)}</h1>
<p class="lead">{lead}</p>
{body}
</div></body></html>"""


def main(folder):
    import os
    uc02 = page("UC02 · Compra parcelada · Sequência", "Diagrama de sequência · GastoCerto · UC02",
                  "Registrar compra parcelada",
                  "Versão de apresentação, em duas partes: a prévia e a confirmação. A fonte formal está em <code>docs/uml/sequencia-uc02-*.puml</code>.",
                  [("Prévia das parcelas", "RN01 · competência pelo fechamento do cartão · RN02 · 2 a 48 parcelas, resto na 1ª", q1),
                   ("Confirmar e gravar", "RN08 · competência CLOSED não aceita lançamentos", q2)])
    uc03 = page("UC03 · Fechar o mês · Sequência", "Diagrama de sequência · GastoCerto · UC03",
                  "Fechar o mês e abrir o seguinte",
                  "Versão de apresentação, em três partes encadeadas. A Parte 3 detalha a chamada <code>expectedForMonth</code> da Parte 2. A fonte formal está em <code>docs/uml/sequencia-uc03-*.puml</code>.",
                  [("Resumo antes de fechar", "RN04 · surplus() = income − (totalExpenses − thirdPartyExpenses)", p1),
                   ("Fechamento numa única transação", "RN08 · competência CLOSED não aceita lançamentos · A3 · previstos não duplicados (mesmo originId)", p2),
                   ("Geração dos previstos (Factory + Creator)", "RN06 contas fixas · RN07 salário · RN11 aportes planejados", p3)])
    for name, html in (("sequencia-uc02.html", uc02), ("sequencia-uc03.html", uc03)):
        open(os.path.join(folder, name), "w", encoding="utf-8").write(html)
    if os.environ.get("CHECK_PANELS"):
        for p in (q1, q2, p1, p2, p3):
            open(os.path.join(os.environ["CHECK_PANELS"], f"check-{p.slug}.html"), "w", encoding="utf-8").write(
                f"<!DOCTYPE html><html lang='pt-BR'><head><meta charset='UTF-8'><title>{p.slug}</title></head><body>{p.svg()}</body></html>")
    print("ok", [p.w for p in (q1, q2, p1, p2, p3)])


if __name__ == "__main__":
    main(sys.argv[1])
