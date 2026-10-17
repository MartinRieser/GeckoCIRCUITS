#!/usr/bin/env python3
"""
Generates publication-quality European / IEC 60617 vector SVG schematics
for all GeckoCIRCUITS tutorial pages.

Zero-gap architecture: Every component lead connects with exact coordinates
to rails, wires, and junctions.
"""
import os
import re

BASE_DIR = "/Users/martinrieser/Documents/gecko/docs/tutorials"

def xml_escape(text):
    return re.sub(r'&(?!(?:[a-zA-Z]+|#\d+|#x[0-9a-fA-F]+);)', '&amp;', text)

def clean_subscripts(text):
    def _sub(match):
        inner = match.group(1)
        inner = inner.replace('_{dc}', 'dc').replace('_{DC}', 'dc').replace('{dc}', 'dc').replace('{DC}', 'dc')
        inner = inner.replace('{', '').replace('}', '')
        return f'>{inner}<'
    return re.sub(r'>([^<]*[\{\}][^<]*)<', _sub, text)

def svg_wrap(viewbox, title, subtitle, elements):
    title = xml_escape(title)
    subtitle = xml_escape(subtitle)
    elements = clean_subscripts(elements)
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="{viewbox}" width="100%" height="100%">
  <defs>
    <style>
      .wire {{ stroke: #1e293b; stroke-width: 2.5; stroke-linecap: round; stroke-linejoin: round; fill: none; }}
      .wire-thin {{ stroke: #64748b; stroke-width: 1.5; stroke-linecap: round; stroke-linejoin: round; fill: none; }}
      .wire-dashed {{ stroke: #0284c7; stroke-width: 2; stroke-dasharray: 5,4; fill: none; }}
      .wire-gnd {{ stroke: #1e293b; stroke-width: 2.5; stroke-linecap: round; }}
      .junction {{ fill: #1e293b; }}
      .label {{ font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; font-size: 14px; fill: #0f172a; font-weight: 600; text-anchor: middle; }}
      .label-start {{ font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; font-size: 14px; fill: #0f172a; font-weight: 600; text-anchor: start; }}
      .label-end {{ font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; font-size: 14px; fill: #0f172a; font-weight: 600; text-anchor: end; }}
      .sublabel {{ font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; font-size: 11px; fill: #64748b; font-weight: 500; text-anchor: middle; }}
      .sublabel-start {{ font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; font-size: 11px; fill: #64748b; font-weight: 500; text-anchor: start; }}
      .title {{ font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; font-size: 16px; fill: #0f172a; font-weight: 700; }}
      .subtitle {{ font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; font-size: 12px; fill: #64748b; font-weight: 400; }}
      .comp-body {{ fill: #ffffff; stroke: #1e293b; stroke-width: 2.2; }}
      .comp-diode {{ fill: #0284c7; stroke: #0284c7; stroke-width: 2; }}
      .comp-sw {{ stroke: #0284c7; stroke-width: 2.5; stroke-linecap: round; fill: none; }}
      .arrow {{ fill: #0284c7; stroke: none; }}
      .measure-arrow {{ stroke: #0284c7; stroke-width: 1.75; fill: none; }}
      .tag-pos {{ fill: #dc2626; font-family: sans-serif; font-size: 16px; font-weight: bold; text-anchor: middle; }}
      .tag-neg {{ fill: #2563eb; font-family: sans-serif; font-size: 18px; font-weight: bold; text-anchor: middle; }}
      .card {{ fill: #ffffff; stroke: #e2e8f0; stroke-width: 1.5; rx: 8px; }}
      .block {{ fill: #f8fafc; stroke: #cbd5e1; stroke-width: 1.5; rx: 6px; }}
      .block-accent {{ fill: #f0f9ff; stroke: #0284c7; stroke-width: 1.5; rx: 6px; }}
    </style>
    <marker id="arr-blue" viewBox="0 0 10 10" refX="7" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
      <path d="M 0 1.5 L 8 5 L 0 8.5 z" fill="#0284c7"/>
    </marker>
    <marker id="arr-green" viewBox="0 0 10 10" refX="7" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
      <path d="M 0 1.5 L 8 5 L 0 8.5 z" fill="#16a34a"/>
    </marker>
  </defs>
  <rect width="100%" height="100%" class="card" />
  <text x="35" y="38" class="title">{title}</text>
  <text x="35" y="58" class="subtitle">{subtitle}</text>
  {elements}
</svg>'''

# ----------------- Core Primitives -----------------

def junc(x, y):
    return f'<circle cx="{x}" cy="{y}" r="4" class="junction" />'

def wire(a, b, c=None, d=None, stroke="#1e293b", stroke_width=2.5, dash=""):
    if d is None:
        # 3-arg call: wire(x1, x2, y) -> horizontal line from (x1, y) to (x2, y)
        x1, y1, x2, y2 = a, c, b, c
    else:
        # 4-arg call: wire(x1, y1, x2, y2)
        x1, y1, x2, y2 = a, b, c, d
    cls = "wire-dashed" if dash else "wire"
    extra = f' stroke="{stroke}"' if stroke != "#1e293b" else ""
    return f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" class="{cls}"{extra} />'

def gnd(x, y, label="GND"):
    return f'''
    <line x1="{x}" y1="{y}" x2="{x}" y2="{y+15}" class="wire" />
    <line x1="{x-18}" y1="{y+15}" x2="{x+18}" y2="{y+15}" class="wire-gnd" />
    <line x1="{x-11}" y1="{y+21}" x2="{x+11}" y2="{y+21}" class="wire-gnd" />
    <line x1="{x-4}" y1="{y+27}" x2="{x+4}" y2="{y+27}" class="wire-gnd" />
    <text x="{x}" y="{y+42}" class="sublabel">{label}</text>
    '''

def terminal(x, y, label="", side="left", color="#0284c7"):
    anchor = "end" if side == "left" else "start"
    dx = -12 if side == "left" else 12
    return f'''
    <circle cx="{x}" cy="{y}" r="4.5" fill="{color}" />
    <text x="{x+dx}" y="{y+4}" class="label" text-anchor="{anchor}" fill="{color}">{label}</text>
    '''

def probe_v(x, y1, y2, label="V_out", polarity="+/-"):
    top_sgn = "+" if polarity == "+/-" else "&#8722;"
    bot_sgn = "&#8722;" if polarity == "+/-" else "+"
    ym = int((y1 + y2) / 2)
    return f'''
    <circle cx="{x}" cy="{y1}" r="4" fill="none" stroke="#0284c7" stroke-width="2" />
    <circle cx="{x}" cy="{y2}" r="4" fill="none" stroke="#0284c7" stroke-width="2" />
    <text x="{x+14}" y="{y1+5}" class="tag-pos">{top_sgn}</text>
    <text x="{x+14}" y="{y2+5}" class="tag-neg">{bot_sgn}</text>
    <line x1="{x+25}" y1="{y1+15}" x2="{x+25}" y2="{y2-15}" class="measure-arrow" marker-end="url(#arr-blue)" />
    <text x="{x+42}" y="{ym+5}" class="label" fill="#0284c7">{label}</text>
    '''

# ----------------- Two-Point Vertical Components -----------------

def source_dc_v(x, y1, y2, label="V_in", val="", pos_top=True):
    ym = int((y1 + y2) / 2)
    top_sgn = "+" if pos_top else "&#8722;"
    bot_sgn = "&#8722;" if pos_top else "+"
    top_cls = "tag-pos" if pos_top else "tag-neg"
    bot_cls = "tag-neg" if pos_top else "tag-pos"
    return f'''
    <line x1="{x}" y1="{y1}" x2="{x}" y2="{ym-22}" class="wire" />
    <circle cx="{x}" cy="{ym}" r="22" class="comp-body" />
    <line x1="{x}" y1="{ym+22}" x2="{x}" y2="{y2}" class="wire" />
    <text x="{x}" y="{ym-6}" class="{top_cls}">{top_sgn}</text>
    <text x="{x}" y="{ym+14}" class="{bot_cls}">{bot_sgn}</text>
    <text x="{x-32}" y="{ym-4}" class="label-end">{label}</text>
    <text x="{x-32}" y="{ym+14}" class="sublabel" text-anchor="end">{val}</text>
    '''

def source_ac_v(x, y1, y2, label="V_ac", val=""):
    ym = int((y1 + y2) / 2)
    return f'''
    <line x1="{x}" y1="{y1}" x2="{x}" y2="{ym-22}" class="wire" />
    <circle cx="{x}" cy="{ym}" r="22" class="comp-body" />
    <line x1="{x}" y1="{ym+22}" x2="{x}" y2="{y2}" class="wire" />
    <path d="M {x-11} {ym} Q {x-5.5} {ym-11} {x} {ym} Q {x+5.5} {ym+11} {x+11} {ym}" class="wire" />
    <text x="{x-32}" y="{ym-4}" class="label-end">{label}</text>
    <text x="{x-32}" y="{ym+14}" class="sublabel" text-anchor="end">{val}</text>
    '''

def res_v(x, y1, y2, label="R", val="", side="right"):
    ym = int((y1 + y2) / 2)
    anchor = "start" if side == "right" else "end"
    dx = 20 if side == "right" else -20
    return f'''
    <line x1="{x}" y1="{y1}" x2="{x}" y2="{ym-28}" class="wire" />
    <rect x="{x-12}" y="{ym-28}" width="24" height="56" class="comp-body" />
    <line x1="{x}" y1="{ym+28}" x2="{x}" y2="{y2}" class="wire" />
    <text x="{x+dx}" y="{ym-3}" class="label-{anchor}">{label}</text>
    <text x="{x+dx}" y="{ym+15}" class="sublabel-{anchor}">{val}</text>
    '''

def cap_v(x, y1, y2, label="C", val="", side="right"):
    ym = int((y1 + y2) / 2)
    anchor = "start" if side == "right" else "end"
    dx = 24 if side == "right" else -24
    return f'''
    <line x1="{x}" y1="{y1}" x2="{x}" y2="{ym-6}" class="wire" />
    <line x1="{x-18}" y1="{ym-6}" x2="{x+18}" y2="{ym-6}" class="wire" stroke-width="3" />
    <line x1="{x-18}" y1="{ym+6}" x2="{x+18}" y2="{ym+6}" class="wire" stroke-width="3" />
    <line x1="{x}" y1="{ym+6}" x2="{x}" y2="{y2}" class="wire" />
    <text x="{x+dx}" y="{ym-3}" class="label-{anchor}">{label}</text>
    <text x="{x+dx}" y="{ym+15}" class="sublabel-{anchor}">{val}</text>
    '''

def ind_v(x, y1, y2, label="L", val="", side="left"):
    ym = int((y1 + y2) / 2)
    anchor = "end" if side == "left" else "start"
    dx = -22 if side == "left" else 22
    return f'''
    <line x1="{x}" y1="{y1}" x2="{x}" y2="{ym-28}" class="wire" />
    <path d="M {x} {ym-28} C {x+16} {ym-28}, {x+16} {ym-14}, {x} {ym-14}
             C {x+16} {ym-14}, {x+16} {ym}, {x} {ym}
             C {x+16} {ym}, {x+16} {ym+14}, {x} {ym+14}
             C {x+16} {ym+14}, {x+16} {ym+28}, {x} {ym+28}" class="wire" stroke-width="2.5" />
    <line x1="{x}" y1="{ym+28}" x2="{x}" y2="{y2}" class="wire" />
    <text x="{x+dx}" y="{ym-3}" class="label-{anchor}">{label}</text>
    <text x="{x+dx}" y="{ym+15}" class="sublabel" text-anchor="{anchor}">{val}</text>
    '''

def diode_v(x, y1, y2, direction="up", label="D", val="", side="right"):
    ym = int((y1 + y2) / 2)
    anchor = "start" if side == "right" else "end"
    dx = 22 if side == "right" else -22
    if direction == "up":
        return f'''
        <line x1="{x}" y1="{y1}" x2="{x}" y2="{ym-14}" class="wire" />
        <line x1="{x-14}" y1="{ym-14}" x2="{x+14}" y2="{ym-14}" class="wire" stroke-width="2.5" stroke="#0284c7" />
        <polygon points="{x-14},{ym+14} {x},{ym-14} {x+14},{ym+14}" class="comp-diode" />
        <line x1="{x}" y1="{ym+14}" x2="{x}" y2="{y2}" class="wire" />
        <text x="{x+dx}" y="{ym-2}" class="label-{anchor}">{label}</text>
        <text x="{x+dx}" y="{ym+16}" class="sublabel-{anchor}">{val}</text>
        '''
    else:
        return f'''
        <line x1="{x}" y1="{y1}" x2="{x}" y2="{ym-14}" class="wire" />
        <polygon points="{x-14},{ym-14} {x},{ym+14} {x+14},{ym-14}" class="comp-diode" />
        <line x1="{x-14}" y1="{ym+14}" x2="{x+14}" y2="{ym+14}" class="wire" stroke-width="2.5" stroke="#0284c7" />
        <line x1="{x}" y1="{ym+14}" x2="{x}" y2="{y2}" class="wire" />
        <text x="{x+dx}" y="{ym-2}" class="label-{anchor}">{label}</text>
        <text x="{x+dx}" y="{ym+16}" class="sublabel-{anchor}">{val}</text>
        '''

def switch_v(x, y1, y2, label="S", val="", with_diode=True, side="left"):
    ym = int((y1 + y2) / 2)
    anchor = "end" if side == "left" else "start"
    dx = -26 if side == "left" else 26
    blade_dir = -14 if side == "left" else 14
    content = f'''
    <line x1="{x}" y1="{y1}" x2="{x}" y2="{ym-18}" class="wire" />
    <circle cx="{x}" cy="{ym-18}" r="3.5" class="junction" />
    <line x1="{x}" y1="{ym-18}" x2="{x+blade_dir}" y2="{ym+10}" class="comp-sw" />
    <circle cx="{x}" cy="{ym+18}" r="3.5" class="junction" />
    <line x1="{x}" y1="{ym+18}" x2="{x}" y2="{y2}" class="wire" />
    <text x="{x+dx}" y="{ym-2}" class="label-{anchor}">{label}</text>
    <text x="{x+dx}" y="{ym+16}" class="sublabel" text-anchor="{anchor}">{val}</text>
    '''
    if with_diode:
        diode_x = x + 35 if side == "left" else x - 35
        content += f'''
        <line x1="{x}" y1="{ym-28}" x2="{diode_x}" y2="{ym-28}" class="wire-thin" />
        <line x1="{diode_x}" y1="{ym-28}" x2="{diode_x}" y2="{ym-12}" class="wire-thin" />
        <polygon points="{diode_x-10},{ym+10} {diode_x},{ym-10} {diode_x+10},{ym+10}" fill="#94a3b8" stroke="#94a3b8" />
        <line x1="{diode_x-10}" y1="{ym-10}" x2="{diode_x+10}" y2="{ym-10}" stroke="#94a3b8" stroke-width="2" />
        <line x1="{diode_x}" y1="{ym+10}" x2="{diode_x}" y2="{ym+28}" class="wire-thin" />
        <line x1="{diode_x}" y1="{ym+28}" x2="{x}" y2="{ym+28}" class="wire-thin" />
        <circle cx="{x}" cy="{ym-28}" r="3" class="junction" />
        <circle cx="{x}" cy="{ym+28}" r="3" class="junction" />
        '''
    return content

# ----------------- Two-Point Horizontal Components -----------------

def res_h(x1, x2, y, label="R", val="", side="top"):
    xm = int((x1 + x2) / 2)
    dy_l = -18 if side == "top" else 26
    dy_s = 26 if side == "top" else -18
    return f'''
    <line x1="{x1}" y1="{y}" x2="{xm-28}" y2="{y}" class="wire" />
    <rect x="{xm-28}" y="{y-12}" width="56" height="24" class="comp-body" />
    <line x1="{xm+28}" y1="{y}" x2="{x2}" y2="{y}" class="wire" />
    <text x="{xm}" y="{y+dy_l}" class="label">{label}</text>
    <text x="{xm}" y="{y+dy_s}" class="sublabel">{val}</text>
    '''

def cap_h(x1, x2, y, label="C", val="", side="top"):
    xm = int((x1 + x2) / 2)
    dy_l = -24 if side == "top" else 32
    dy_s = 32 if side == "top" else -24
    return f'''
    <line x1="{x1}" y1="{y}" x2="{xm-6}" y2="{y}" class="wire" />
    <line x1="{xm-6}" y1="{y-18}" x2="{xm-6}" y2="{y+18}" class="wire" stroke-width="3" />
    <line x1="{xm+6}" y1="{y-18}" x2="{xm+6}" y2="{y+18}" class="wire" stroke-width="3" />
    <line x1="{xm+6}" y1="{y}" x2="{x2}" y2="{y}" class="wire" />
    <text x="{xm}" y="{y+dy_l}" class="label">{label}</text>
    <text x="{xm}" y="{y+dy_s}" class="sublabel">{val}</text>
    '''

def ind_h(x1, x2, y, label="L", val="", side="top"):
    xm = int((x1 + x2) / 2)
    dy_l = -22 if side == "top" else 28
    dy_s = 24 if side == "top" else -20
    return f'''
    <line x1="{x1}" y1="{y}" x2="{xm-28}" y2="{y}" class="wire" />
    <path d="M {xm-28} {y} C {xm-28} {y-16}, {xm-14} {y-16}, {xm-14} {y}
             C {xm-14} {y-16}, {xm} {y-16}, {xm} {y}
             C {xm} {y-16}, {xm+14} {y-16}, {xm+14} {y}
             C {xm+14} {y-16}, {xm+28} {y-16}, {xm+28} {y}" class="wire" stroke-width="2.5" />
    <line x1="{xm+28}" y1="{y}" x2="{x2}" y2="{y}" class="wire" />
    <text x="{xm}" y="{y+dy_l}" class="label">{label}</text>
    <text x="{xm}" y="{y+dy_s}" class="sublabel">{val}</text>
    '''

def diode_h(x1, x2, y, direction="right", label="D", val="", side="top"):
    xm = int((x1 + x2) / 2)
    dy_l = -22 if side == "top" else 26
    dy_s = 26 if side == "top" else -20
    if direction == "right":
        return f'''
        <line x1="{x1}" y1="{y}" x2="{xm-14}" y2="{y}" class="wire" />
        <polygon points="{xm-14},{y-14} {xm+14},{y} {xm-14},{y+14}" class="comp-diode" />
        <line x1="{xm+14}" y1="{y-14}" x2="{xm+14}" y2="{y+14}" class="wire" stroke-width="2.5" stroke="#0284c7" />
        <line x1="{xm+14}" y1="{y}" x2="{x2}" y2="{y}" class="wire" />
        <text x="{xm}" y="{y+dy_l}" class="label">{label}</text>
        <text x="{xm}" y="{y+dy_s}" class="sublabel">{val}</text>
        '''
    else:
        return f'''
        <line x1="{x1}" y1="{y}" x2="{xm-14}" y2="{y}" class="wire" />
        <line x1="{xm-14}" y1="{y-14}" x2="{xm-14}" y2="{y+14}" class="wire" stroke-width="2.5" stroke="#0284c7" />
        <polygon points="{xm+14},{y-14} {xm-14},{y} {xm+14},{y+14}" class="comp-diode" />
        <line x1="{xm+14}" y1="{y}" x2="{x2}" y2="{y}" class="wire" />
        <text x="{xm}" y="{y+dy_l}" class="label">{label}</text>
        <text x="{xm}" y="{y+dy_s}" class="sublabel">{val}</text>
        '''

def save(rel_path, content):
    full_path = os.path.join(BASE_DIR, rel_path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, "w") as f:
        f.write(content)
    print(f"Created {rel_path}")

# ==============================================================================
# 1. SEPIC Converter
# ==============================================================================
def gen_sepic():
    elems = [
        source_dc_v(120, 140, 290, "V_in", "DC Input"),
        junc(120, 140), junc(120, 290),
        ind_h(120, 250, 140, "L_1", "Input Inductor"),
        junc(250, 140),
        switch_v(250, 140, 290, "S", "PWM Switch", with_diode=False),
        junc(250, 290),
        cap_h(250, 400, 140, "C_cp", "Coupling Cap"),
        junc(400, 140),
        ind_v(400, 140, 290, "L_2", "Second Inductor"),
        junc(400, 290),
        diode_h(400, 550, 140, direction="right", label="D", val="Freewheel Diode"),
        junc(550, 140),
        cap_v(550, 140, 290, "C_out", "Output Filter"),
        junc(550, 290),
        wire(550, 140, 670, 140),
        junc(670, 140),
        res_v(670, 140, 290, "R_L", "Load"),
        junc(670, 290),
        wire(670, 140, 740, 140),
        wire(670, 290, 740, 290),
        probe_v(740, 140, 290, "Vdc"),
        wire(120, 290, 670, 290),
        gnd(325, 290),
    ]
    return svg_wrap("0 0 880 390", "SEPIC Converter (Step-Up / Step-Down)",
                    "European IEC 60617 Standard Schematic • Non-Inverting DC-DC Topology",
                    "\n".join(elems))

# ==============================================================================
# 2. Ćuk Converter
# ==============================================================================
def gen_cuk():
    elems = [
        source_dc_v(120, 140, 290, "V_in", "DC Input"),
        junc(120, 140), junc(120, 290),
        ind_h(120, 250, 140, "L_1", "Input Inductor"),
        junc(250, 140),
        switch_v(250, 140, 290, "S", "PWM Switch", with_diode=False),
        junc(250, 290),
        cap_h(250, 400, 140, "C_1", "Transfer Cap"),
        junc(400, 140),
        diode_v(400, 140, 290, direction="up", label="D", val="Freewheel Diode"),
        junc(400, 290),
        ind_h(400, 550, 140, "L_2", "Output Inductor"),
        junc(550, 140),
        cap_v(550, 140, 290, "C_2", "Filter Cap"),
        junc(550, 290),
        wire(550, 140, 670, 140),
        junc(670, 140),
        res_v(670, 140, 290, "R_L", "Load"),
        junc(670, 290),
        wire(670, 140, 740, 140),
        wire(670, 290, 740, 290),
        probe_v(740, 140, 290, "&#8722;Vdc", polarity="-/+"),
        wire(120, 290, 670, 290),
        gnd(325, 290),
    ]
    return svg_wrap("0 0 880 390", "Ćuk Converter (Inverting Step-Up/Step-Down)",
                    "European IEC 60617 Standard Schematic • Continuous Input & Output Current",
                    "\n".join(elems))

# ==============================================================================
# 3. Single-Phase Full-Wave Diode Bridge Rectifier
# ==============================================================================
def gen_single_phase_rectifier():
    elems = [
        source_ac_v(120, 160, 280, "V_ac", "Mains 230V / 50Hz"),
        # AC Line 1 (top): (120, 160) to (260, 160) down to Leg 1 midpoint (260, 220)
        wire(120, 160, 260, 160),
        wire(260, 160, 260, 220),
        junc(260, 220),
        # AC Line 2 (bot): (120, 280) to (380, 280) up to Leg 2 midpoint (380, 220)
        wire(120, 280, 380, 280),
        wire(380, 280, 380, 220),
        junc(380, 220),

        # Leg 1 at x=260:
        diode_v(260, 100, 220, direction="up", label="D_1"),
        diode_v(260, 220, 340, direction="up", label="D_2"),

        # Leg 2 at x=380:
        diode_v(380, 100, 220, direction="up", label="D_3"),
        diode_v(380, 220, 340, direction="up", label="D_4"),

        # DC+ Rail at y=100 (Red)
        wire(260, 680, 100, stroke="#dc2626"),
        junc(260, 100), junc(380, 100), junc(490, 100), junc(590, 100),
        f'<text x="695" y="105" class="tag-pos">+</text>',

        # DC- Rail at y=340 (Blue)
        wire(260, 680, 340, stroke="#2563eb"),
        junc(260, 340), junc(380, 340), junc(490, 340), junc(590, 340),
        f'<text x="695" y="345" class="tag-neg">&#8722;</text>',

        # Smoothing Capacitor C_f at x=490 from y=100 to y=340
        cap_v(490, 100, 340, "C_f", "Smoothing Cap"),

        # Load Resistor R_L at x=590 from y=100 to y=340
        res_v(590, 100, 340, "R_L", "DC Load"),

        # Output probe at x=680 from y=100 to y=340
        probe_v(680, 100, 340, "Vdc"),

        # Ground reference at (490, 340)
        gnd(490, 340, "0V / GND"),
    ]
    return svg_wrap("0 0 860 420", "Single-Phase Full-Wave Diode Bridge Rectifier",
                    "European IEC 60617 Standard Schematic • Uncontrolled Full-Wave AC-DC Converter",
                    "\n".join(elems))

# ==============================================================================
# 4. Three-Phase Diode Bridge Rectifier
# ==============================================================================
def gen_three_phase_rectifier():
    elems = [
        # AC Inputs
        terminal(80, 160, "L_1", side="left"),
        wire(80, 160, 260, 160),
        wire(260, 160, 260, 220),
        junc(260, 220),

        terminal(80, 220, "L_2", side="left"),
        wire(80, 220, 370, 220),
        junc(370, 220),

        terminal(80, 280, "L_3", side="left"),
        wire(80, 280, 480, 280),
        wire(480, 280, 480, 220),
        junc(480, 220),

        # Leg 1 (Phase A, x=260)
        diode_v(260, 100, 220, direction="up", label="D_1"),
        diode_v(260, 220, 340, direction="up", label="D_4"),

        # Leg 2 (Phase B, x=370)
        diode_v(370, 100, 220, direction="up", label="D_3"),
        diode_v(370, 220, 340, direction="up", label="D_6"),

        # Leg 3 (Phase C, x=480)
        diode_v(480, 100, 220, direction="up", label="D_5"),
        diode_v(480, 220, 340, direction="up", label="D_2"),

        # DC+ Rail (Red)
        wire(260, 750, 100, stroke="#dc2626"),
        junc(260, 100), junc(370, 100), junc(480, 100), junc(590, 100), junc(680, 100),
        f'<text x="765" y="105" class="tag-pos">+</text>',

        # DC- Rail (Blue)
        wire(260, 750, 340, stroke="#2563eb"),
        junc(260, 340), junc(370, 340), junc(480, 340), junc(590, 340), junc(680, 340),
        f'<text x="765" y="345" class="tag-neg">&#8722;</text>',

        # Smoothing Capacitor C_dc at x=590
        cap_v(590, 100, 340, "C_dc", "Bulk Filter"),

        # Load Resistor R_L at x=680
        res_v(680, 100, 340, "R_L", "DC Load"),

        # Probe at x=750
        probe_v(750, 100, 340, "Vdc"),
        gnd(590, 340, "0V / GND"),
    ]
    return svg_wrap("0 0 880 430", "Three-Phase 6-Pulse Diode Bridge Rectifier (B6)",
                    "European IEC 60617 Standard Schematic • Symmetrical 6-Pulse Full-Wave Rectification",
                    "\n".join(elems))

# ==============================================================================
# 5. Boost PFC
# ==============================================================================
def gen_boost_pfc():
    elems = [
        # AC Mains Source (x=100, y=160 to y=300)
        source_ac_v(100, 160, 300, "V_mains", "230V / 50Hz"),
        wire(100, 160, 180, 160),
        wire(100, 300, 180, 300),
        junc(180, 160), junc(180, 300),

        # EMI Filter Cap C_in (x=180)
        cap_v(180, 160, 300, "C_in", "EMI Filter"),

        # Connect to Bridge Rectifier block (x=240..300, y=140..320)
        wire(180, 160, 240, 160),
        wire(180, 300, 240, 300),
        f'<rect x="240" y="140" width="60" height="180" class="block" />',
        f'<text x="270" y="225" class="label">Bridge</text>',
        f'<text x="270" y="245" class="sublabel">Rectifier</text>',

        # Output of Rectifier (x=300):
        # 0V Return Rail (Bottom, y=300)
        wire(300, 780, 300),
        # Boost Inductor L_pfc from (300, 160) to (440, 160)
        ind_h(300, 440, 160, "L_pfc", "Boost Inductor"),
        junc(440, 160),

        # Switch S_pfc at x=440
        switch_v(440, 160, 300, "S_pfc", "MOSFET / IGBT"),
        junc(440, 300),

        # Boost Diode D_pfc
        diode_h(440, 560, 160, direction="right", label="D_pfc", val="SiC Schottky"),
        junc(560, 160),

        # Wire from D_pfc to C_out and R_L
        wire(560, 160, 650, 160),
        junc(650, 160),
        cap_v(650, 160, 300, "C_out", "Bulk 400V"),
        junc(650, 300),

        wire(650, 160, 730, 160),
        junc(730, 160),
        res_v(730, 160, 300, "R_L", "Load"),
        junc(730, 300),

        wire(730, 160, 780, 160),
        probe_v(780, 160, 300, "Vdc = 400V"),
        gnd(440, 300, "0V Return"),
    ]
    return svg_wrap("0 0 900 400", "Active Boost Power Factor Correction (PFC) Converter",
                    "European IEC 60617 Standard Schematic • Sinusoidal Input Current Shaping",
                    "\n".join(elems))

# ==============================================================================
# 6. Vienna Rectifier
# ==============================================================================
def gen_vienna_rectifier():
    elems = [
        # AC Inputs through inductors L_a, L_b, L_c
        terminal(80, 160, "L_1", side="left"),
        ind_h(80, 220, 160, "L_a", ""),
        junc(220, 160),

        terminal(80, 240, "L_2", side="left"),
        ind_h(80, 310, 240, "L_b", ""),
        junc(310, 240),

        terminal(80, 320, "L_3", side="left"),
        ind_h(80, 400, 320, "L_c", ""),
        junc(400, 320),

        # Phase Legs:
        # Leg A at x=220:
        diode_v(220, 90, 160, direction="up", label="D_1a"),
        diode_v(220, 160, 390, direction="up", label="D_2a"),
        # Switch A to Neutral (y=240):
        wire(220, 160, 250, 160),
        wire(250, 160, 250, 240),
        wire(250, 240, 480, 240),

        # Leg B at x=310:
        diode_v(310, 90, 240, direction="up", label="D_1b"),
        diode_v(310, 240, 390, direction="up", label="D_2b"),

        # Leg C at x=400:
        diode_v(400, 90, 320, direction="up", label="D_1c"),
        diode_v(400, 320, 390, direction="up", label="D_2c"),
        # Switch C to Neutral (y=240):
        wire(400, 320, 430, 320),
        wire(430, 320, 430, 240),
        wire(430, 240, 480, 240),

        # DC+ Rail (y=90)
        wire(220, 800, 90, stroke="#dc2626"),
        junc(220, 90), junc(310, 90), junc(400, 90), junc(660, 90), junc(740, 90),
        f'<text x="815" y="95" class="tag-pos">+Vdc / 2</text>',

        # Neutral Rail (y=240)
        wire(250, 800, 240, stroke="#64748b"),
        junc(310, 240), junc(430, 240), junc(660, 240),
        f'<text x="815" y="245" class="sublabel-start">Neutral (M)</text>',

        # DC- Rail (y=390)
        wire(220, 800, 390, stroke="#2563eb"),
        junc(220, 390), junc(310, 390), junc(400, 390), junc(660, 390), junc(740, 390),
        f'<text x="815" y="395" class="tag-neg">&#8722;Vdc / 2</text>',

        # Split Capacitors C1 and C2 at x=660
        cap_v(660, 90, 240, "C_1", "Upper Bus"),
        cap_v(660, 240, 390, "C_2", "Lower Bus"),
        gnd(660, 240, "Neutral GND"),

        # Load Resistor R_L at x=740
        res_v(740, 90, 390, "R_L", "DC Load"),

        # Total Probe at x=800
        probe_v(800, 90, 390, "Vdc"),
    ]
    return svg_wrap("0 0 920 480", "Three-Phase Three-Level Vienna Rectifier",
                    "European IEC 60617 Standard Schematic • Low THD & Three-Level DC Output",
                    "\n".join(elems))

# ==============================================================================
# 7. Half-Bridge Inverter
# ==============================================================================
def gen_half_bridge_inverter():
    elems = [
        # DC Rails: +Vdc/2 (y=100), Neutral (y=220), -Vdc/2 (y=340)
        wire(120, 360, 100, stroke="#dc2626"),
        f'<text x="105" y="105" class="tag-pos">+Vdc/2</text>',
        wire(120, 680, 340, stroke="#2563eb"),
        f'<text x="105" y="345" class="tag-neg">&#8722;Vdc/2</text>',

        # Split Capacitors at x=190:
        junc(190, 100), junc(190, 220), junc(190, 340),
        cap_v(190, 100, 220, "C_1", "Upper Bus"),
        cap_v(190, 220, 340, "C_2", "Lower Bus"),

        # Neutral line from (190, 220) down to (190, 280) and horizontally across to AC load
        wire(190, 220, 190, 280),
        wire(190, 280, 680, 280),
        gnd(260, 280, "Neutral GND"),

        # Switch Leg at x=360:
        junc(360, 100), junc(360, 220), junc(360, 340),
        switch_v(360, 100, 220, "S_1", "Upper Switch"),
        switch_v(360, 220, 340, "S_2", "Lower Switch"),

        # Filter Inductor L_f from switch midpoint (360, 220) to (500, 220)
        ind_h(360, 500, 220, "L_f", "Filter Inductor"),
        junc(500, 220),

        # Filter Capacitor C_f at x=500 from y=220 down to Neutral (280)
        cap_v(500, 220, 280, "C_f", "Filter Cap"),
        junc(500, 280),

        # Load Resistor R_load at x=600 from y=220 down to Neutral (280)
        wire(500, 220, 600, 220),
        junc(600, 220),
        res_v(600, 220, 280, "R_load", "AC Load"),
        junc(600, 280),

        # Output probe across (680, 220) and (680, 280)
        wire(600, 220, 680, 220),
        probe_v(680, 220, 280, "V_AC = &#177; Vdc/2"),
    ]
    return svg_wrap("0 0 880 430", "Single-Phase Half-Bridge Inverter",
                    "European IEC 60617 Standard Schematic • Two-Quadrant Switching Leg with LC Output Filter",
                    "\n".join(elems))

# ==============================================================================
# 8. Full-Bridge Inverter (H-Bridge)
# ==============================================================================
def gen_full_bridge_inverter():
    elems = [
        # DC Rails: +Vdc (y=100), 0V/GND (y=340)
        wire(120, 480, 100, stroke="#dc2626"),
        f'<text x="105" y="105" class="tag-pos">+Vdc</text>',
        wire(120, 480, 340, stroke="#2563eb"),
        f'<text x="105" y="345" class="tag-neg">0V</text>',

        # DC-link Capacitor C_dc at x=180
        junc(180, 100), junc(180, 340),
        cap_v(180, 100, 340, "C_dc", "DC-Link Cap"),
        gnd(180, 340, "GND"),

        # Leg 1 at x=300: S1, S2
        junc(300, 100), junc(300, 220), junc(300, 340),
        switch_v(300, 100, 220, "S_1", ""),
        switch_v(300, 220, 340, "S_2", ""),

        # Leg 2 at x=450: S3, S4
        junc(450, 100), junc(450, 220), junc(450, 340),
        switch_v(450, 100, 220, "S_3", ""),
        switch_v(450, 220, 340, "S_4", ""),

        # AC Output Terminals & Load:
        # Leg 1 midpoint (300, 220) routed up to y=170 and across to x=580
        wire(300, 220, 300, 170),
        wire(300, 170, 580, 170),

        # Leg 2 midpoint (450, 220) routed down to y=270 and across to x=580
        wire(450, 220, 450, 270),
        wire(450, 270, 580, 270),

        # Load Resistor R_load at x=580 between y=170 and y=270
        junc(580, 170), junc(580, 270),
        res_v(580, 170, 270, "R_load", "AC Load"),

        # Output probe at x=650
        wire(580, 170, 650, 170),
        wire(580, 270, 650, 270),
        probe_v(650, 170, 270, "V_out = &#177; Vdc"),
    ]
    return svg_wrap("0 0 880 430", "Single-Phase Full-Bridge (H-Bridge) Inverter",
                    "European IEC 60617 Standard Schematic • 4-Quadrant Bipolar/Unipolar PWM Inverter",
                    "\n".join(elems))

# ==============================================================================
# 9. Three-Phase Inverter
# ==============================================================================
def gen_three_phase_inverter():
    elems = [
        # DC Rails: +Vdc (y=100), 0V (y=340)
        wire(120, 560, 100, stroke="#dc2626"),
        f'<text x="105" y="105" class="tag-pos">+Vdc</text>',
        wire(120, 560, 340, stroke="#2563eb"),
        f'<text x="105" y="345" class="tag-neg">0V</text>',

        # DC bus capacitor C_dc at x=180
        junc(180, 100), junc(180, 340),
        cap_v(180, 100, 340, "C_dc", "DC Bus Cap"),
        gnd(180, 340, "GND"),

        # Leg A at x=280:
        junc(280, 100), junc(280, 220), junc(280, 340),
        switch_v(280, 100, 220, "S_1", ""),
        switch_v(280, 220, 340, "S_4", ""),
        wire(280, 220, 680, 220),
        terminal(680, 220, "Phase A (U)", side="right"),

        # Leg B at x=400:
        junc(400, 100), junc(400, 220), junc(400, 340),
        switch_v(400, 100, 220, "S_3", ""),
        switch_v(400, 220, 340, "S_6", ""),
        wire(400, 220, 400, 250),
        wire(400, 250, 680, 250),
        terminal(680, 250, "Phase B (V)", side="right"),

        # Leg C at x=520:
        junc(520, 100), junc(520, 220), junc(520, 340),
        switch_v(520, 100, 220, "S_5", ""),
        switch_v(520, 220, 340, "S_2", ""),
        wire(520, 220, 520, 280),
        wire(520, 280, 680, 280),
        terminal(680, 280, "Phase C (W)", side="right"),
    ]
    return svg_wrap("0 0 880 430", "Three-Phase Voltage Source Inverter (2-Level VSI)",
                    "European IEC 60617 Standard Schematic • 6-Switch Bridge for AC Motor Drives & Grid Tie",
                    "\n".join(elems))

# ==============================================================================
# 10. FOC Control Diagram
# ==============================================================================
def gen_foc_diagram():
    elems = [
        # Motor
        f'<circle cx="780" cy="200" r="45" class="block-accent" />',
        f'<text x="780" y="195" class="label" fill="#0284c7">PMSM</text>',
        f'<text x="780" y="215" class="sublabel">Motor</text>',

        # Inverter Block
        f'<rect x="610" y="155" width="80" height="90" class="block" />',
        f'<text x="650" y="195" class="label">3-Phase</text>',
        f'<text x="650" y="215" class="sublabel">Inverter</text>',
        wire(690, 200, 735, 200),
        f'<polygon points="735,200 725,195 725,205" fill="#1e293b" />',

        # SVM Block
        f'<rect x="490" y="165" width="70" height="70" class="block" />',
        f'<text x="525" y="200" class="label">SVM</text>',
        f'<text x="525" y="218" class="sublabel">PWM Gen</text>',
        wire(560, 200, 610, 200),
        f'<polygon points="610,200 600,195 600,205" fill="#1e293b" />',

        # Inverse Park Transform
        f'<rect x="360" y="165" width="80" height="70" class="block" />',
        f'<text x="400" y="195" class="label">dq &#8594; &#945;&#946;</text>',
        f'<text x="400" y="215" class="sublabel">Inv. Park</text>',
        wire(440, 200, 490, 200),
        f'<polygon points="490,200 480,195 480,205" fill="#1e293b" />',

        # Current Controllers (PI)
        f'<rect x="230" y="145" width="80" height="45" class="block" />',
        f'<text x="270" y="172" class="label">PI (d-axis)</text>',
        wire(310, 167, 360, 185),

        f'<rect x="230" y="210" width="80" height="45" class="block" />',
        f'<text x="270" y="237" class="label">PI (q-axis)</text>',
        wire(310, 232, 360, 215),

        # Feedback Park Transform
        f'<rect x="360" y="280" width="80" height="60" class="block" />',
        f'<text x="400" y="305" class="label">&#945;&#946; &#8594; dq</text>',
        f'<text x="400" y="325" class="sublabel">Park</text>',

        # Clarke Transform
        f'<rect x="490" y="280" width="80" height="60" class="block" />',
        f'<text x="530" y="305" class="label">abc &#8594; &#945;&#946;</text>',
        f'<text x="530" y="325" class="sublabel">Clarke</text>',
        wire(490, 310, 440, 310),
        f'<polygon points="440,310 450,305 450,315" fill="#1e293b" />',

        # Current measurement feedback wire
        wire(710, 200, 710, 310),
        junc(710, 200),
        wire(710, 310, 570, 310),
        f'<polygon points="570,310 580,305 580,315" fill="#1e293b" />',

        # dq feedback loops
        wire(360, 310, 150, 310),
        wire(150, 310, 150, 167),
        wire(150, 167, 230, 167),
        f'<polygon points="230,167 220,162 220,172" fill="#1e293b" />',

        wire(180, 310, 180, 232),
        junc(180, 310),
        wire(180, 232, 230, 232),
        f'<polygon points="230,232 220,227 220,237" fill="#1e293b" />',

        # Reference inputs
        terminal(80, 150, "i_d* = 0", side="left", color="#16a34a"),
        wire(80, 150, 230, 150),

        terminal(80, 220, "i_q* (Torque)", side="left", color="#16a34a"),
        wire(80, 220, 230, 220),
    ]
    return svg_wrap("0 0 880 390", "Field-Oriented Control (FOC) Architecture",
                    "Direct / Quadrature (dq0) Decoupled Vector Control with Space Vector Modulation",
                    "\n".join(elems))

# ==============================================================================
# 11. NPC Inverter Phase Leg
# ==============================================================================
def gen_npc_leg():
    elems = [
        # DC Rails: +Vdc/2 (y=80), Neutral (y=240), -Vdc/2 (y=400)
        wire(100, 450, 80, stroke="#dc2626"),
        f'<text x="65" y="85" class="tag-pos">+Vdc / 2</text>',
        wire(100, 310, 240, stroke="#64748b"),
        f'<text x="65" y="245" class="sublabel-start">Neutral (M)</text>',
        wire(100, 450, 400, stroke="#2563eb"),
        f'<text x="65" y="405" class="tag-neg">&#8722;Vdc / 2</text>',

        # Split capacitors C1 and C2 at x=180
        junc(180, 80), junc(180, 240), junc(180, 400),
        cap_v(180, 80, 240, "C_1", "Upper DC Cap"),
        cap_v(180, 240, 400, "C_2", "Lower DC Cap"),
        gnd(180, 240, "Neutral Ref"),

        # Neutral wire extending to clamping branch at x=310
        wire(180, 240, 310, 240),
        junc(310, 240),

        # Clamping Diodes at x=310:
        # Upper clamp D_c1: from Neutral (240) up to S1/S2 node (160)
        diode_v(310, 160, 240, direction="up", label="D_c1", val="Upper Clamp", side="left"),
        wire(310, 160, 450, 160),

        # Lower clamp D_c2: from S3/S4 node (320) up to Neutral (240)
        diode_v(310, 240, 320, direction="up", label="D_c2", val="Lower Clamp", side="left"),
        wire(310, 320, 450, 320),

        # 4 Switches in Series at x=450:
        junc(450, 80), junc(450, 160), junc(450, 240), junc(450, 320), junc(450, 400),
        switch_v(450, 80, 160, "S_1", ""),
        switch_v(450, 160, 240, "S_2", ""),
        switch_v(450, 240, 320, "S_3", ""),
        switch_v(450, 320, 400, "S_4", ""),

        # AC Output from midpoint (450, 240) to (640, 240)
        wire(450, 240, 640, 240, stroke="#0284c7", stroke_width=3),
        terminal(640, 240, "AC Output (3-Level)", side="right"),
        f'<text x="655" y="260" class="sublabel-start">+Vdc/2, 0, &#8722;Vdc/2</text>',
    ]
    return svg_wrap("0 0 920 460", "Three-Level Neutral-Point-Clamped (NPC) Inverter Leg",
                    "European IEC 60617 Standard Schematic • Reduced dv/dt Stress & 3-Level Voltage Output",
                    "\n".join(elems))

# ==============================================================================
# 12. MMC Topology
# ==============================================================================
def gen_mmc_topology():
    elems = [
        # DC Rails: +Vdc/2 (y=90), -Vdc/2 (y=410)
        wire(150, 720, 90, stroke="#dc2626"),
        f'<text x="130" y="95" class="tag-pos">+Vdc / 2</text>',
        wire(150, 720, 410, stroke="#2563eb"),
        f'<text x="130" y="415" class="tag-neg">&#8722;Vdc / 2</text>',

        # Three Phase Legs: A (x=260), B (x=440), C (x=620)
        # Leg A
        junc(260, 90), junc(260, 410),
        wire(260, 90, 260, 115),
        f'<rect x="230" y="115" width="60" height="26" class="block-accent" /><text x="260" y="133" class="sublabel">SM 1</text>',
        wire(260, 141, 260, 150),
        f'<rect x="230" y="150" width="60" height="26" class="block-accent" /><text x="260" y="168" class="sublabel">SM 2</text>',
        wire(260, 176, 260, 185),
        f'<text x="260" y="195" class="label">&#8942;</text>',
        wire(260, 198, 260, 205),
        f'<rect x="230" y="205" width="60" height="26" class="block-accent" /><text x="260" y="223" class="sublabel">SM N</text>',
        ind_v(260, 231, 275, "L_arm", ""),
        junc(260, 275),
        wire(260, 275, 200, 275),
        terminal(200, 275, "Phase A", side="left"),

        ind_v(260, 275, 319, "L_arm", ""),
        f'<rect x="230" y="319" width="60" height="26" class="block-accent" /><text x="260" y="337" class="sublabel">SM 1</text>',
        wire(260, 345, 260, 355),
        f'<text x="260" y="367" class="label">&#8942;</text>',
        wire(260, 370, 260, 375),
        f'<rect x="230" y="375" width="60" height="26" class="block-accent" /><text x="260" y="393" class="sublabel">SM N</text>',
        wire(260, 401, 260, 410),

        # Leg B
        junc(440, 90), junc(440, 410),
        wire(440, 90, 440, 115),
        f'<rect x="410" y="115" width="60" height="26" class="block-accent" /><text x="440" y="133" class="sublabel">SM 1</text>',
        wire(440, 141, 440, 150),
        f'<rect x="410" y="150" width="60" height="26" class="block-accent" /><text x="440" y="168" class="sublabel">SM 2</text>',
        wire(440, 176, 440, 185),
        f'<text x="440" y="195" class="label">&#8942;</text>',
        wire(440, 198, 440, 205),
        f'<rect x="410" y="205" width="60" height="26" class="block-accent" /><text x="440" y="223" class="sublabel">SM N</text>',
        ind_v(440, 231, 275, "L_arm", ""),
        junc(440, 275),
        wire(440, 275, 500, 275),
        terminal(500, 275, "Phase B", side="right"),

        ind_v(440, 275, 319, "L_arm", ""),
        f'<rect x="410" y="319" width="60" height="26" class="block-accent" /><text x="440" y="337" class="sublabel">SM 1</text>',
        wire(440, 345, 440, 355),
        f'<text x="440" y="367" class="label">&#8942;</text>',
        wire(440, 370, 440, 375),
        f'<rect x="410" y="375" width="60" height="26" class="block-accent" /><text x="440" y="393" class="sublabel">SM N</text>',
        wire(440, 401, 440, 410),

        # Leg C
        junc(620, 90), junc(620, 410),
        wire(620, 90, 620, 115),
        f'<rect x="590" y="115" width="60" height="26" class="block-accent" /><text x="620" y="133" class="sublabel">SM 1</text>',
        wire(620, 141, 620, 150),
        f'<rect x="590" y="150" width="60" height="26" class="block-accent" /><text x="620" y="168" class="sublabel">SM 2</text>',
        wire(620, 176, 620, 185),
        f'<text x="620" y="195" class="label">&#8942;</text>',
        wire(620, 198, 620, 205),
        f'<rect x="590" y="205" width="60" height="26" class="block-accent" /><text x="620" y="223" class="sublabel">SM N</text>',
        ind_v(620, 231, 275, "L_arm", ""),
        junc(620, 275),
        wire(620, 275, 680, 275),
        terminal(680, 275, "Phase C", side="right"),

        ind_v(620, 275, 319, "L_arm", ""),
        f'<rect x="590" y="319" width="60" height="26" class="block-accent" /><text x="620" y="337" class="sublabel">SM 1</text>',
        wire(620, 345, 620, 355),
        f'<text x="620" y="367" class="label">&#8942;</text>',
        wire(620, 370, 620, 375),
        f'<rect x="590" y="375" width="60" height="26" class="block-accent" /><text x="620" y="393" class="sublabel">SM N</text>',
        wire(620, 401, 620, 410),
    ]
    return svg_wrap("0 0 880 470", "Modular Multilevel Converter (MMC) Topology",
                    "European IEC 60617 Standard Schematic • Cascaded Submodule HVDC & STATCOM Architecture",
                    "\n".join(elems))

# ==============================================================================
# 13. MMC Submodules
# ==============================================================================
def gen_mmc_submodules():
    elems = [
        # Half-Bridge Submodule (Left, x=180..380)
        f'<rect x="80" y="90" width="340" height="250" class="card" />',
        f'<text x="250" y="115" class="label" fill="#0284c7">Half-Bridge Submodule (HBSM)</text>',
        f'<text x="250" y="132" class="sublabel">2-Level (0, +V_sm)</text>',

        # HBSM Capacitor at x=160
        junc(160, 150), junc(160, 310),
        cap_v(160, 150, 310, "C_sm", ""),
        wire(160, 150, 260, 150),
        wire(160, 310, 380, 310),

        # HBSM Switches at x=260: S1 (150 to 230), S2 (230 to 310)
        junc(260, 150), junc(260, 230), junc(260, 310),
        switch_v(260, 150, 230, "S_1", ""),
        switch_v(260, 230, 310, "S_2", ""),

        # Terminals: Out 1 from (260, 230) to (380, 230); Out 2 from (260, 310) to (380, 310)
        wire(260, 230, 380, 230),
        terminal(380, 230, "Terminal 1", side="right"),
        terminal(380, 310, "Terminal 2", side="right"),

        # Full-Bridge Submodule (Right, x=480..840)
        f'<rect x="460" y="90" width="400" height="250" class="card" />',
        f'<text x="660" y="115" class="label" fill="#0284c7">Full-Bridge Submodule (FBSM)</text>',
        f'<text x="660" y="132" class="sublabel">3-Level (&#8722;V_sm, 0, +V_sm) • DC Fault Clearing</text>',

        # FBSM Capacitor at x=530
        junc(530, 150), junc(530, 310),
        cap_v(530, 150, 310, "C_sm", ""),
        wire(530, 150, 730, 150),
        wire(530, 310, 730, 310),

        # Leg 1 at x=630: S1, S2
        junc(630, 150), junc(630, 220), junc(630, 310),
        switch_v(630, 150, 220, "S_1", ""),
        switch_v(630, 220, 310, "S_2", ""),

        # Leg 2 at x=730: S3, S4
        junc(730, 150), junc(730, 250), junc(730, 310),
        switch_v(730, 150, 250, "S_3", ""),
        switch_v(730, 250, 310, "S_4", ""),

        # Terminals: Out 1 from (630, 220) to (820, 220); Out 2 from (730, 250) to (820, 250)
        wire(630, 220, 820, 220),
        wire(730, 250, 820, 250),
        terminal(820, 220, "Terminal 1", side="right"),
        terminal(820, 250, "Terminal 2", side="right"),
    ]
    return svg_wrap("0 0 900 370", "MMC Submodule Configurations (HBSM vs. FBSM)",
                    "European IEC 60617 Standard Schematic • Multilevel Arm Building Blocks",
                    "\n".join(elems))

# ==============================================================================
# 14. Thermal Resistance Chain
# ==============================================================================
def gen_thermal_chain():
    elems = [
        f'<rect x="60" y="130" width="130" height="70" class="block-accent" />',
        f'<text x="125" y="160" class="label" fill="#0284c7">Junction</text>',
        f'<text x="125" y="180" class="sublabel">T_j (Die)</text>',

        f'<line x1="190" y1="165" x2="270" y2="165" class="wire" marker-end="url(#arr-blue)" />',
        f'<text x="230" y="155" class="label">R_th,jc</text>',

        f'<rect x="270" y="130" width="130" height="70" class="block" />',
        f'<text x="335" y="160" class="label">Case</text>',
        f'<text x="335" y="180" class="sublabel">T_c (Package)</text>',

        f'<line x1="400" y1="165" x2="480" y2="165" class="wire" marker-end="url(#arr-blue)" />',
        f'<text x="440" y="155" class="label">R_th,ch</text>',

        f'<rect x="480" y="130" width="130" height="70" class="block" />',
        f'<text x="545" y="160" class="label">Heatsink</text>',
        f'<text x="545" y="180" class="sublabel">T_h</text>',

        f'<line x1="610" y1="165" x2="690" y2="165" class="wire" marker-end="url(#arr-blue)" />',
        f'<text x="650" y="155" class="label">R_th,ha</text>',

        f'<rect x="690" y="130" width="130" height="70" class="block-accent" />',
        f'<text x="755" y="160" class="label" fill="#0284c7">Ambient</text>',
        f'<text x="755" y="180" class="sublabel">T_a (Air)</text>',

        f'<line x1="125" y1="75" x2="125" y2="125" class="wire" stroke="#dc2626" marker-end="url(#arr-blue)" stroke-width="3" />',
        f'<text x="125" y="65" class="label" fill="#dc2626">P_loss (W)</text>',
    ]
    return svg_wrap("0 0 880 260", "Semiconductor Heat Transfer Chain",
                    "One-Dimensional Thermal Flow from Silicon Die to Ambient Environment",
                    "\n".join(elems))

# ==============================================================================
# 15. Thermal Networks: Foster vs. Cauer
# ==============================================================================
def gen_thermal_networks():
    elems = [
        # Foster Network (Top half)
        f'<text x="40" y="78" class="label-start" fill="#0284c7">Foster Thermal Network (Mathematical Series of RC Pairs)</text>',
        # Power Loss Source Ploss
        f'<circle cx="80" cy="175" r="18" class="comp-body" />',
        f'<text x="80" y="180" class="label">P</text>',
        wire(80, 157, 80, 140),
        wire(80, 140, 165, 140),
        wire(80, 193, 80, 215),
        wire(80, 215, 700, 215),

        # Stage 1: R1 || C1 (x=210)
        res_h(165, 255, 115, "R_1", ""),
        cap_h(165, 255, 165, "C_1", ""),
        wire(165, 115, 165, 165),
        junc(165, 140),
        wire(255, 115, 255, 165),
        junc(255, 140),
        wire(255, 140, 325, 140),

        # Stage 2: R2 || C2 (x=370)
        res_h(325, 415, 115, "R_2", ""),
        cap_h(325, 415, 165, "C_2", ""),
        wire(325, 115, 325, 165),
        junc(325, 140),
        wire(415, 115, 415, 165),
        junc(415, 140),
        wire(415, 140, 485, 140),

        # Stage 3: R3 || C3 (x=530)
        res_h(485, 575, 115, "R_3", ""),
        cap_h(485, 575, 165, "C_3", ""),
        wire(485, 115, 485, 165),
        junc(485, 140),
        wire(575, 115, 575, 165),
        junc(575, 140),
        wire(575, 140, 700, 140),
        probe_v(700, 140, 215, "T_j - T_a"),
        gnd(600, 215, "T_ambient"),

        # Cauer Network (Bottom half)
        f'<text x="40" y="265" class="label-start" fill="#0284c7">Cauer Thermal Network (Physical Ladder with Shunt Capacitances)</text>',
        f'<circle cx="80" cy="355" r="18" class="comp-body" />',
        f'<text x="80" y="360" class="label">P</text>',
        wire(80, 337, 80, 305),
        wire(80, 305, 165, 305),
        wire(80, 373, 80, 415),
        wire(80, 415, 700, 415),

        # Ladder R1, C1, R2, C2, R3, C3
        res_h(165, 260, 305, "R_1", "Die"),
        junc(260, 305),
        cap_v(260, 305, 415, "C_1", ""),
        junc(260, 415),

        res_h(260, 410, 305, "R_2", "Solder"),
        junc(410, 305),
        cap_v(410, 305, 415, "C_2", ""),
        junc(410, 415),

        res_h(410, 560, 305, "R_3", "Heatsink"),
        junc(560, 305),
        cap_v(560, 305, 415, "C_3", ""),
        junc(560, 415),
        wire(560, 305, 700, 305),
        gnd(560, 415, "T_ambient"),
    ]
    return svg_wrap("0 0 880 470", "Dynamic Thermal Impedance Networks: Foster vs. Cauer",
                    "European IEC 60617 Standard Schematic • RC Models for Transient Junction Temperature",
                    "\n".join(elems))

# ==============================================================================
# 16. Practical Transformer Model
# ==============================================================================
def gen_transformer_equivalent():
    elems = [
        # Primary terminals (x=80)
        terminal(80, 150, "1", side="left"),
        terminal(80, 300, "2", side="left"),
        probe_v(50, 150, 300, "V_1"),

        # Primary series: Rp, L_sigma_p
        res_h(80, 200, 150, "R_p", "Winding Res."),
        ind_h(200, 320, 150, "L_&#963;p", "Leakage Ind."),
        junc(320, 150),

        # Magnetizing branch at x=370:
        wire(320, 150, 370, 150),
        # Parallel R_fe (x=340) and L_m (x=400)
        wire(370, 150, 340, 150),
        res_v(340, 150, 300, "R_fe", "Core Loss", side="left"),
        wire(340, 300, 370, 300),

        wire(370, 150, 400, 150),
        ind_v(400, 150, 300, "L_m", "Magnetizing", side="right"),
        wire(400, 300, 370, 300),
        junc(370, 150), junc(370, 300),

        # Ideal Transformer (x=450..490)
        wire(370, 150, 450, 150),
        wire(80, 300, 450, 300),
        junc(370, 300),

        # Transformer coils
        f'<path d="M 450 150 C 470 150, 470 180, 450 180 C 470 180, 470 210, 450 210 C 470 210, 470 240, 450 240 C 470 240, 470 270, 450 270 C 470 270, 470 300, 450 300" class="wire" />',
        f'<line x1="468" y1="140" x2="468" y2="310" class="wire-thin" stroke-width="2" />',
        f'<line x1="472" y1="140" x2="472" y2="310" class="wire-thin" stroke-width="2" />',
        f'<path d="M 490 150 C 470 150, 470 180, 490 180 C 470 180, 470 210, 490 210 C 470 210, 470 240, 490 240 C 470 240, 470 270, 490 270 C 470 270, 470 300, 490 300" class="wire" />',
        f'<circle cx="438" cy="160" r="3.5" fill="#0f172a" />',
        f'<circle cx="502" cy="160" r="3.5" fill="#0f172a" />',
        f'<text x="470" y="335" class="label">N_1 : N_2</text>',

        # Secondary series: L_sigma_s, R_s
        ind_h(490, 610, 150, "L_&#963;s", "Sec. Leakage"),
        res_h(610, 730, 150, "R_s", "Sec. Winding"),
        wire(730, 150, 780, 150),
        wire(490, 300, 780, 300),

        # Secondary terminals
        terminal(780, 150, "1'", side="right"),
        terminal(780, 300, "2'", side="right"),
        probe_v(800, 150, 300, "V_2"),
    ]
    return svg_wrap("0 0 900 400", "Practical Transformer Electrical Equivalent Circuit",
                    "European IEC 60617 Standard Schematic • Winding Resistances, Leakage & Magnetizing Core Losses",
                    "\n".join(elems))

# ==============================================================================
# 17. Permeance Network
# ==============================================================================
def gen_permeance_network():
    elems = [
        # Magnetic rail top & bot
        wire(160, 700, 130),
        wire(160, 700, 310),
        junc(160, 130), junc(160, 310),

        # MMF1 Source at x=160
        source_dc_v(160, 130, 310, "F_1", "N_1 &#183; i_1"),

        # P_sigma1 at x=280
        junc(280, 130), junc(280, 310),
        res_v(280, 130, 310, "P_&#963;1", "Leakage"),

        # P_core / P_gap at x=430
        junc(430, 130), junc(430, 310),
        res_v(430, 130, 220, "P_core", "Core"),
        junc(430, 220),
        res_v(430, 220, 310, "P_gap", "Air Gap"),

        # P_sigma2 at x=570
        junc(570, 130), junc(570, 310),
        res_v(570, 130, 310, "P_&#963;2", "Leakage"),

        # MMF2 Source at x=700
        junc(700, 130), junc(700, 310),
        source_dc_v(700, 130, 310, "F_2", "N_2 &#183; i_2"),
    ]
    return svg_wrap("0 0 880 390", "Magnetic Circuit Permeance Network (Hopkinson's Analogy)",
                    "Permeance Model with Core, Air Gap and Primary/Secondary Leakage Paths",
                    "\n".join(elems))

# ==============================================================================
# 18. Magnetic Coupling / Gyrator
# ==============================================================================
def gen_magnetic_coupling():
    elems = [
        # Left side: Electrical Domain
        f'<rect x="60" y="100" width="320" height="240" class="card" />',
        f'<text x="220" y="130" class="label">Electrical Domain</text>',
        terminal(100, 170, "i(t)", side="left"),
        wire(100, 170, 200, 170),
        terminal(100, 270, "&#8722;", side="left"),
        wire(100, 270, 200, 270),
        ind_v(200, 170, 270, "N", "Turns"),
        f'<text x="140" y="225" class="label" fill="#0284c7">v(t)</text>',
        f'<line x1="140" y1="185" x2="140" y2="255" class="measure-arrow" marker-end="url(#arr-blue)" />',

        # Coupling Interface Box (Center)
        f'<rect x="350" y="150" width="160" height="140" class="block-accent" />',
        f'<text x="430" y="185" class="label" fill="#0284c7">Winding Interface</text>',
        f'<text x="430" y="210" class="sublabel">F = N &#183; i</text>',
        f'<text x="430" y="235" class="sublabel">v = N &#183; d&#934;/dt</text>',
        f'<text x="430" y="260" class="sublabel">(Gyrator Coupling)</text>',

        # Right side: Magnetic Domain
        f'<rect x="480" y="100" width="340" height="240" class="card" />',
        f'<text x="650" y="130" class="label">Magnetic Domain</text>',
        source_dc_v(560, 170, 270, "F", "MMF"),
        wire(560, 170, 700, 170),
        wire(560, 270, 700, 270),
        junc(560, 170), junc(560, 270),
        res_v(700, 170, 270, "P_core", "Core"),
        junc(700, 170), junc(700, 270),
        f'<line x1="600" y1="160" x2="650" y2="160" class="measure-arrow" marker-end="url(#arr-green)" />',
        f'<text x="625" y="150" class="sublabel" fill="#16a34a">&#934;(t) Flux</text>',
    ]
    return svg_wrap("0 0 880 390", "Magnetic-Electrical Dual Domain Interface",
                    "European IEC 60617 Standard Schematic • Winding Gyrator Transduction Model",
                    "\n".join(elems))

# ==============================================================================
# 19. Two-Mass Torsional Drive Train
# ==============================================================================
def gen_two_mass_system():
    elems = [
        f'<rect x="120" y="140" width="120" height="120" class="block-accent" />',
        f'<text x="180" y="195" class="label" fill="#0284c7">Rotor Inertia</text>',
        f'<text x="180" y="215" class="sublabel">J_1 (kg&#183;m&#178;)</text>',

        wire(50, 200, 120, 200),
        f'<polygon points="120,200 110,195 110,205" fill="#1e293b" />',
        f'<text x="75" y="185" class="label" fill="#dc2626">T_e(t)</text>',
        f'<text x="75" y="225" class="sublabel">Elec. Torque</text>',

        wire(240, 200, 350, 200),
        f'<rect x="350" y="170" width="160" height="60" class="comp-body" />',
        f'<text x="430" y="198" class="label">Flexible Shaft</text>',
        f'<text x="430" y="216" class="sublabel">k (Nm/rad), d (Nm&#183;s/rad)</text>',

        wire(510, 200, 620, 200),
        f'<rect x="620" y="140" width="120" height="120" class="block" />',
        f'<text x="680" y="195" class="label">Load Inertia</text>',
        f'<text x="680" y="215" class="sublabel">J_2 (kg&#183;m&#178;)</text>',

        wire(740, 200, 810, 200),
        f'<polygon points="810,200 800,195 800,205" fill="#1e293b" />',
        f'<text x="780" y="185" class="label" fill="#2563eb">T_L(t)</text>',
        f'<text x="780" y="225" class="sublabel">Load Torque</text>',
    ]
    return svg_wrap("0 0 880 340", "Two-Mass Torsional Mechanical Drive Train Model",
                    "European IEC 60617 Standard Schematic • Motor Rotor, Flexible Shaft & Load Dynamics",
                    "\n".join(elems))

# ==============================================================================
# 20. Inductor Saturation Characteristics Curve
# ==============================================================================
def gen_saturation_curve():
    elems = [
        wire(140, 80, 140, 300),
        wire(140, 300, 750, 300),
        f'<polygon points="140,75 135,85 145,85" fill="#1e293b" />',
        f'<polygon points="755,300 745,295 745,305" fill="#1e293b" />',
        f'<text x="140" y="65" class="label">Differential Inductance L(i)</text>',
        f'<text x="710" y="325" class="label">Current i (A)</text>',

        wire(140, 120, 700, 120, dash="5,4"),
        f'<text x="125" y="125" class="label-end">L_nom</text>',
        wire(140, 260, 700, 260, dash="5,4"),
        f'<text x="125" y="265" class="label-end">L_sat</text>',

        wire(380, 120, 380, 300, dash="5,4"),
        f'<text x="380" y="325" class="label">I_sat</text>',

        # Piecewise Linear Curve (blue dashed)
        f'<path d="M 140 120 L 380 120 L 380 260 L 700 260" class="wire-dashed" stroke-width="2.5" />',
        f'<text x="450" y="105" class="sublabel-start" fill="#0284c7">Piecewise Linear Model</text>',

        # Smooth Saturation Curve (dark line)
        f'<path d="M 140 120 C 300 120, 350 140, 380 190 C 410 240, 480 260, 700 260" class="wire" stroke-width="3" />',
        f'<text x="560" y="200" class="label-start">Smooth Roll-Off Model</text>',
        f'<text x="560" y="220" class="sublabel-start" fill="#dc2626">L(i) = L_0 / [1 + (i / I_sat)^n]</text>',
    ]
    return svg_wrap("0 0 860 380", "Inductor Magnetic Core Saturation Characteristics",
                    "Differential Inductance vs. Winding Current: Piecewise Linear & Smooth Saturation Models",
                    "\n".join(elems))

def main():
    save("dcdc/sepic-converter-schematic.svg", gen_sepic())
    save("dcdc/cuk-converter-schematic.svg", gen_cuk())
    save("acdc/single-phase-rectifier-schematic.svg", gen_single_phase_rectifier())
    save("acdc/three-phase-rectifier-schematic.svg", gen_three_phase_rectifier())
    save("acdc/boost-pfc-schematic.svg", gen_boost_pfc())
    save("acdc/vienna-rectifier-schematic.svg", gen_vienna_rectifier())
    save("dcac/half-bridge-inverter-schematic.svg", gen_half_bridge_inverter())
    save("dcac/full-bridge-inverter-schematic.svg", gen_full_bridge_inverter())
    save("dcac/three-phase-inverter-schematic.svg", gen_three_phase_inverter())
    save("dcac/foc-control-diagram.svg", gen_foc_diagram())
    save("dcac/npc-phase-leg-schematic.svg", gen_npc_leg())
    save("dcac/mmc-topology-schematic.svg", gen_mmc_topology())
    save("dcac/mmc-submodules-schematic.svg", gen_mmc_submodules())
    save("thermal/thermal-resistance-chain.svg", gen_thermal_chain())
    save("thermal/thermal-network-schematic.svg", gen_thermal_networks())
    save("magnetics/transformer-equivalent-schematic.svg", gen_transformer_equivalent())
    save("magnetics/permeance-network-schematic.svg", gen_permeance_network())
    save("magnetics/magnetic-coupling-schematic.svg", gen_magnetic_coupling())
    save("magnetics/two-mass-system-schematic.svg", gen_two_mass_system())
    save("magnetics/inductor-saturation-curve.svg", gen_saturation_curve())
    print("\nAll 20 vector SVGs successfully generated!")

if __name__ == "__main__":
    main()
