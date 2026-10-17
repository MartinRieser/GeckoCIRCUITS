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
      .sublabel-end {{ font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; font-size: 11px; fill: #64748b; font-weight: 500; text-anchor: end; }}
      .title {{ font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; font-size: 16px; fill: #0f172a; font-weight: 700; }}
      .subtitle {{ font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; font-size: 12px; fill: #64748b; font-weight: 400; }}
      .comp-body {{ fill: #ffffff; stroke: #1e293b; stroke-width: 2.2; }}
      .comp-diode {{ fill: #0284c7; stroke: #0284c7; stroke-width: 2; }}
      .comp-sw {{ stroke: #0284c7; stroke-width: 2.5; stroke-linecap: round; fill: none; }}
      .arrow {{ fill: #0284c7; stroke: none; }}
      .measure-arrow {{ stroke: #0284c7; stroke-width: 1.75; fill: none; }}
      .tag-pos {{ fill: #dc2626; font-family: sans-serif; font-size: 16px; font-weight: bold; text-anchor: middle; }}
      .tag-pos-start {{ fill: #dc2626; font-family: sans-serif; font-size: 15px; font-weight: bold; text-anchor: start; }}
      .tag-pos-end {{ fill: #dc2626; font-family: sans-serif; font-size: 15px; font-weight: bold; text-anchor: end; }}
      .tag-neg {{ fill: #2563eb; font-family: sans-serif; font-size: 18px; font-weight: bold; text-anchor: middle; }}
      .tag-neg-start {{ fill: #2563eb; font-family: sans-serif; font-size: 15px; font-weight: bold; text-anchor: start; }}
      .tag-neg-end {{ fill: #2563eb; font-family: sans-serif; font-size: 15px; font-weight: bold; text-anchor: end; }}
      .card {{ fill: #ffffff; stroke: #e2e8f0; stroke-width: 1.5; rx: 8px; }}
      .block {{ fill: #f8fafc; stroke: #cbd5e1; stroke-width: 1.5; rx: 6px; }}
      .block-accent {{ fill: #f0f9ff; stroke: #0284c7; stroke-width: 1.5; rx: 6px; }}
      .leg-box {{ fill: #f8fafc; stroke: #cbd5e1; stroke-width: 1.2; stroke-dasharray: 4,4; rx: 6px; }}
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
    <text x="{x+dx}" y="{y+4}" class="label-{anchor}" fill="{color}">{label}</text>
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
    <text x="{x+42}" y="{ym+5}" class="label-start" fill="#0284c7">{label}</text>
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
    dx = -24 if side == "left" else 24
    coil_dx = 14 if side == "left" else -14
    return f'''
    <line x1="{x}" y1="{y1}" x2="{x}" y2="{ym-28}" class="wire" />
    <path d="M {x} {ym-28} C {x+coil_dx} {ym-28}, {x+coil_dx} {ym-14}, {x} {ym-14}
             C {x+coil_dx} {ym-14}, {x+coil_dx} {ym}, {x} {ym}
             C {x+coil_dx} {ym}, {x+coil_dx} {ym+14}, {x} {ym+14}
             C {x+coil_dx} {ym+14}, {x+coil_dx} {ym+28}, {x} {ym+28}" class="wire" stroke-width="2.5" />
    <line x1="{x}" y1="{ym+28}" x2="{x}" y2="{y2}" class="wire" />
    <text x="{x+dx}" y="{ym-3}" class="label-{anchor}">{label}</text>
    <text x="{x+dx}" y="{ym+15}" class="sublabel-{anchor}">{val}</text>
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
    <text x="{x+dx}" y="{ym+16}" class="sublabel-{anchor}">{val}</text>
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

def hop_h(x1, x_hop, x2, y, r=6, stroke="#1e293b"):
    cls = "wire"
    extra = f' stroke="{stroke}"' if stroke != "#1e293b" else ""
    return f'''
    <line x1="{x1}" y1="{y}" x2="{x_hop-r}" y2="{y}" class="{cls}"{extra} />
    <path d="M {x_hop-r} {y} A {r} {r} 0 0 1 {x_hop+r} {y}" class="{cls}"{extra} />
    <line x1="{x_hop+r}" y1="{y}" x2="{x2}" y2="{y}" class="{cls}"{extra} />
    '''

def switch_bidir_h(x1, x2, y, label="S", val="Bidirectional"):
    xm = int((x1 + x2) / 2)
    return f'''
    <line x1="{x1}" y1="{y}" x2="{xm-18}" y2="{y}" class="wire" />
    <circle cx="{xm-18}" cy="{y}" r="3" fill="#0284c7" />
    <circle cx="{xm+18}" cy="{y}" r="3" fill="#0284c7" />
    <line x1="{xm-16}" y1="{y-1}" x2="{xm+14}" y2="{y-14}" class="comp-sw" />
    <line x1="{xm-12}" y1="{y+10}" x2="{xm+12}" y2="{y+10}" stroke="#0284c7" stroke-width="1.5" marker-start="url(#arr-blue)" marker-end="url(#arr-blue)" />
    <line x1="{xm+18}" y1="{y}" x2="{x2}" y2="{y}" class="wire" />
    <text x="{xm}" y="{y-18}" class="label">{label}</text>
    <text x="{xm}" y="{y+24}" class="sublabel" fill="#0284c7">{val}</text>
    '''

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
        # AC Line 1 (top): (120, 160) to (180, 160) down to (180, 200) across to Leg 1 phase node (260, 200)
        wire(120, 160, 180, 160),
        wire(180, 160, 180, 200),
        wire(180, 200, 260, 200),
        junc(260, 200),

        # AC Line 2 (bot): (120, 280) to (180, 280) up to (180, 240) across hopping over Leg 1 to Leg 2 (380, 240)
        wire(120, 280, 180, 280),
        wire(180, 280, 180, 240),
        hop_h(180, 260, 380, 240),
        junc(380, 240),

        # Leg 1 at x=260:
        diode_v(260, 100, 180, direction="up", label="D_1"),
        wire(260, 180, 260, 260),
        diode_v(260, 260, 340, direction="up", label="D_2"),

        # Leg 2 at x=380:
        diode_v(380, 100, 180, direction="up", label="D_3"),
        wire(380, 180, 380, 260),
        diode_v(380, 260, 340, direction="up", label="D_4"),

        # DC+ Rail at y=100 (Red)
        wire(260, 680, 100, stroke="#dc2626"),
        junc(260, 100), junc(380, 100), junc(490, 100), junc(590, 100),

        # DC- Rail at y=340 (Blue)
        wire(260, 680, 340, stroke="#2563eb"),
        junc(260, 340), junc(380, 340), junc(490, 340), junc(590, 340),

        # Smoothing Capacitor C_f at x=490 from y=100 to y=340
        cap_v(490, 100, 340, "C_f", "Smoothing Cap"),

        # Load Resistor R_L at x=590 from y=100 to y=340
        res_v(590, 100, 340, "R_L", "DC Load"),

        # Output probe at x=680 from y=100 to y=340
        probe_v(680, 100, 340, "Vdc"),

        # Ground reference at (490, 340)
        gnd(490, 340, "0V / GND"),
    ]
    return svg_wrap("0 0 780 420", "Single-Phase Full-Wave Diode Bridge Rectifier",
                    "European IEC 60617 Standard Schematic • Uncontrolled Full-Wave AC-DC Converter",
                    "\n".join(elems))

# ==============================================================================
# 4. Three-Phase Diode Bridge Rectifier
# ==============================================================================
def gen_three_phase_rectifier():
    elems = [
        # AC Inputs
        terminal(80, 200, "L_1", side="left"),
        wire(80, 200, 260, 200),
        junc(260, 200),

        terminal(80, 220, "L_2", side="left"),
        hop_h(80, 260, 370, 220),
        junc(370, 220),

        terminal(80, 240, "L_3", side="left"),
        hop_h(80, 260, 364, 240),
        hop_h(364, 370, 480, 240),
        junc(480, 240),

        # Leg 1 (Phase A, x=260)
        diode_v(260, 100, 180, direction="up", label="D_1"),
        wire(260, 180, 260, 260),
        diode_v(260, 260, 340, direction="up", label="D_4"),

        # Leg 2 (Phase B, x=370)
        diode_v(370, 100, 180, direction="up", label="D_3"),
        wire(370, 180, 370, 260),
        diode_v(370, 260, 340, direction="up", label="D_6"),

        # Leg 3 (Phase C, x=480)
        diode_v(480, 100, 180, direction="up", label="D_5"),
        wire(480, 180, 480, 260),
        diode_v(480, 260, 340, direction="up", label="D_2"),

        # DC+ Rail (Red)
        wire(260, 760, 100, stroke="#dc2626"),
        junc(260, 100), junc(370, 100), junc(480, 100), junc(590, 100), junc(680, 100),

        # DC- Rail (Blue)
        wire(260, 760, 340, stroke="#2563eb"),
        junc(260, 340), junc(370, 340), junc(480, 340), junc(590, 340), junc(680, 340),

        # Smoothing Capacitor C_dc at x=590
        cap_v(590, 100, 340, "C_dc", "Bulk Filter"),

        # Load Resistor R_L at x=680
        res_v(680, 100, 340, "R_L", "DC Load"),

        # Probe at x=760
        probe_v(760, 100, 340, "Vdc"),
        gnd(590, 340, "0V / GND"),
    ]
    return svg_wrap("0 0 860 430", "Three-Phase 6-Pulse Diode Bridge Rectifier (B6)",
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
        # Phase leg shading boxes (subtle grouping)
        '<rect x="205" y="65" width="130" height="370" class="leg-box" />',
        '<text x="270" y="450" class="sublabel" font-weight="600">Phase Leg A</text>',
        '<rect x="365" y="65" width="130" height="370" class="leg-box" />',
        '<text x="430" y="450" class="sublabel" font-weight="600">Phase Leg B</text>',
        '<rect x="525" y="65" width="130" height="370" class="leg-box" />',
        '<text x="590" y="450" class="sublabel" font-weight="600">Phase Leg C</text>',

        # DC+ Rail (Red, y=80)
        wire(240, 80, 980, 80, stroke="#dc2626"),
        junc(240, 80), junc(400, 80), junc(560, 80), junc(720, 80), junc(840, 80),
        '<circle cx="980" cy="80" r="4.5" fill="#dc2626" />',
        '<text x="995" y="86" class="tag-pos-start">+Vdc / 2 (+350V)</text>',

        # DC- Rail (Blue, y=420)
        wire(240, 420, 980, 420, stroke="#2563eb"),
        junc(240, 420), junc(400, 420), junc(560, 420), junc(720, 420), junc(840, 420),
        '<circle cx="980" cy="420" r="4.5" fill="#2563eb" />',
        '<text x="995" y="426" class="tag-neg-start">&#8722;Vdc / 2 (&#8722;350V)</text>',

        # Neutral Rail M running horizontally at y=325 (cleanly separated from switches at y=280)
        wire(320, 280, 320, 325, stroke="#64748b"),
        junc(320, 325),
        hop_h(320, 400, 480, 325, stroke="#64748b"),
        wire(480, 280, 480, 325, stroke="#64748b"),
        junc(480, 325),
        hop_h(480, 560, 640, 325, stroke="#64748b"),
        wire(640, 280, 640, 325, stroke="#64748b"),
        junc(640, 325),
        wire(640, 325, 685, 325, stroke="#64748b"),
        wire(685, 325, 685, 250, stroke="#64748b"),
        wire(685, 250, 720, 250, stroke="#64748b"),
        junc(720, 250),

        # ================= Phase Leg A =================
        terminal(60, 150, "L_1", side="left"),
        ind_h(60, 165, 150, "L_a", ""),
        wire(165, 150, 240, 150),
        junc(240, 150),

        diode_v(240, 80, 150, direction="up", label="D_1a", side="left"),
        wire(240, 150, 240, 345),
        diode_v(240, 345, 420, direction="up", label="D_2a", side="left"),
        junc(240, 280),
        switch_bidir_h(240, 320, 280, "S_a", "Bi-dir Switch"),

        # ================= Phase Leg B =================
        terminal(60, 195, "L_2", side="left"),
        ind_h(60, 165, 195, "L_b", ""),
        hop_h(165, 240, 400, 195),
        junc(400, 195),

        diode_v(400, 80, 150, direction="up", label="D_1b", side="left"),
        wire(400, 150, 400, 345),
        diode_v(400, 345, 420, direction="up", label="D_2b", side="left"),
        junc(400, 280),
        switch_bidir_h(400, 480, 280, "S_b", "Bi-dir Switch"),

        # ================= Phase Leg C =================
        terminal(60, 240, "L_3", side="left"),
        ind_h(60, 165, 240, "L_c", ""),
        hop_h(165, 240, 394, 240),
        hop_h(394, 400, 560, 240),
        junc(560, 240),

        diode_v(560, 80, 150, direction="up", label="D_1c", side="left"),
        wire(560, 150, 560, 345),
        diode_v(560, 345, 420, direction="up", label="D_2c", side="left"),
        junc(560, 280),
        switch_bidir_h(560, 640, 280, "S_c", "Bi-dir Switch"),

        # ================= DC Link Stage =================
        wire(720, 80, 720, 100),
        cap_v(720, 100, 210, "C_1", "Upper Cap"),
        wire(720, 210, 720, 290),
        cap_v(720, 290, 400, "C_2", "Lower Cap"),
        wire(720, 400, 720, 420),

        # Neutral Ground GND at x=780 (spacious free area)
        wire(720, 250, 780, 250, stroke="#64748b"),
        gnd(780, 250, "Neutral M (0V)"),
        junc(780, 250),

        # Load Resistor RL at x=840
        wire(840, 80, 840, 150),
        res_v(840, 150, 350, "R_L", "DC Load"),
        wire(840, 350, 840, 420),

        # Output Voltage Probe Vdc at x=920
        '<circle cx="920" cy="80" r="3.5" fill="none" stroke="#0284c7" stroke-width="2" />',
        '<circle cx="920" cy="420" r="3.5" fill="none" stroke="#0284c7" stroke-width="2" />',
        '<line x1="920" y1="92" x2="920" y2="236" class="measure-arrow" marker-start="url(#arr-blue)" />',
        '<line x1="920" y1="264" x2="920" y2="408" class="measure-arrow" marker-end="url(#arr-blue)" />',
        '<rect x="896" y="238" width="48" height="24" rx="4" fill="#ffffff" stroke="#0284c7" stroke-width="1.2" />',
        '<text x="920" y="255" class="label" fill="#0284c7" font-size="12px">Vdc</text>',
    ]
    return svg_wrap("0 0 1150 480", "Three-Phase Three-Level Vienna Rectifier",
                    "European IEC 60617 Standard Schematic • Low THD PFC &amp; Three-Level Clamped DC Output",
                    "\n".join(elems))

# ==============================================================================
# 7. Half-Bridge Inverter
# ==============================================================================
def gen_half_bridge_inverter():
    elems = [
        # DC Rails: +Vdc/2 (y=80), -Vdc/2 (y=400)
        wire(150, 80, 360, 80, stroke="#dc2626"),
        '<circle cx="150" cy="80" r="4.5" fill="#dc2626" />',
        '<text x="135" y="85" class="tag-pos-end">+Vdc / 2</text>',

        wire(150, 400, 360, 400, stroke="#2563eb"),
        '<circle cx="150" cy="400" r="4.5" fill="#2563eb" />',
        '<text x="135" y="405" class="tag-neg-end">&#8722;Vdc / 2</text>',

        # Split Capacitors at x=210:
        junc(210, 80), junc(210, 400),
        cap_v(210, 80, 240, "C_1", "Upper Bus", side="left"),
        cap_v(210, 240, 400, "C_2", "Lower Bus", side="left"),
        junc(210, 240),

        # Neutral line from (210, 240) to (280, 240), then drops down to y=365
        wire(210, 240, 280, 240, stroke="#64748b"),
        wire(280, 240, 280, 365, stroke="#64748b"),

        # Neutral horizontal wire at y=365:
        # Hop over Leg A bare wire at (360, 365):
        hop_h(280, 360, 420, 365, stroke="#64748b"),
        wire(420, 365, 800, 365, stroke="#64748b"),
        gnd(450, 365, "Neutral (0V)"),
        junc(590, 365), junc(700, 365),
        terminal(800, 365, "&#8722;", side="right"),

        # Switch Leg at x=360:
        junc(360, 80), junc(360, 400),
        switch_v(360, 80, 200, "S_1", "Upper Switch", side="right"),
        wire(360, 200, 360, 240),
        junc(360, 240),
        wire(360, 240, 360, 260),
        switch_v(360, 260, 340, "S_2", "Lower Switch", side="right"),
        wire(360, 340, 360, 400),

        # Phase Node A leaves (360, 240), jogs right to x=480 (safely past S_1 text!)
        wire(360, 240, 480, 240),
        wire(480, 240, 480, 150),

        # Filter Inductor L_f from (480, 150) to (590, 150)
        ind_h(480, 590, 150, "L_f", "Filter Inductor"),
        wire(590, 150, 800, 150),
        junc(590, 150), junc(700, 150),
        terminal(800, 150, "+", side="right"),

        # Filter Capacitor C_f at x=590 from y=150 down to Neutral (y=365)
        cap_v(590, 150, 365, "C_f", "Filter Cap"),

        # AC Load Resistor R_load at x=700 from y=150 down to Neutral (y=365)
        res_v(700, 150, 365, "R_load", "AC Load"),

        # Output probe at x=820 between y=150 (Phase) and y=365 (Neutral)
        probe_v(820, 150, 365, "V_out = &#177;Vdc / 2"),
    ]
    return svg_wrap("0 0 1020 460", "Single-Phase Half-Bridge Inverter",
                    "European IEC 60617 Standard Schematic • Two-Quadrant Switching Leg with LC Output Filter",
                    "\n".join(elems))

# ==============================================================================
# 8. Full-Bridge Inverter (H-Bridge)
# ==============================================================================
def gen_full_bridge_inverter():
    elems = [
        # DC Rails: +Vdc (y=80), 0V/GND (y=400)
        wire(150, 80, 500, 80, stroke="#dc2626"),
        '<circle cx="150" cy="80" r="4.5" fill="#dc2626" />',
        '<text x="135" y="85" class="tag-pos-end">+Vdc</text>',

        wire(150, 400, 500, 400, stroke="#2563eb"),
        '<circle cx="150" cy="400" r="4.5" fill="#2563eb" />',
        '<text x="135" y="405" class="tag-neg-end">0V</text>',

        # DC-link Capacitor C_dc at x=210
        junc(210, 80), junc(210, 400),
        cap_v(210, 80, 400, "C_dc", "DC-Link Cap", side="left"),
        gnd(210, 400, "GND"),

        # Leg 1 at x=340: S1 (130..220), S2 (260..350)
        junc(340, 80), junc(340, 400),
        wire(340, 80, 340, 130),
        switch_v(340, 130, 220, "S_1", "", side="left"),
        wire(340, 220, 340, 240),
        junc(340, 240),
        wire(340, 240, 340, 260),
        switch_v(340, 260, 350, "S_2", "", side="left"),
        wire(340, 350, 340, 400),

        # Leg 2 at x=500: S3 (130..220), S4 (260..350)
        junc(500, 80), junc(500, 400),
        wire(500, 80, 500, 130),
        switch_v(500, 130, 220, "S_3", "", side="left"),
        wire(500, 220, 500, 240),
        junc(500, 240),
        wire(500, 240, 500, 260),
        switch_v(500, 260, 350, "S_4", "", side="left"),
        wire(500, 350, 500, 400),

        # Phase A: Leg 1 midpoint (340, 240) stepped up to y=105
        # Leg 2 is bare wire between y=80 and y=130!
        # Hop over Leg 2 bare wire at (500, 105):
        wire(340, 240, 380, 240),
        wire(380, 240, 380, 105),
        hop_h(380, 500, 690, 105, stroke="#0284c7"),
        junc(690, 105),
        '<text x="675" y="98" class="sublabel-end" font-weight="bold">Phase A</text>',
        wire(690, 105, 750, 105, stroke="#0284c7"),
        terminal(750, 105, "+", side="right"),

        # Phase B: Leg 2 midpoint (500, 240) stepped down to y=315
        # Jog out to x=580 to safely bypass S_4 diode at x=535
        wire(500, 240, 580, 240),
        wire(580, 240, 580, 315),
        wire(580, 315, 750, 315, stroke="#0284c7"),
        junc(690, 315),
        '<text x="675" y="330" class="sublabel-end" font-weight="bold">Phase B</text>',
        terminal(750, 315, "&#8722;", side="right"),

        # Load Resistor R_load at x=690 between y=105 and y=315
        res_v(690, 105, 315, "R_load", "AC Load"),

        # Output probe at x=770 between y=105 and y=315
        probe_v(770, 105, 315, "V_out = &#177;Vdc"),
    ]
    return svg_wrap("0 0 960 460", "Single-Phase Full-Bridge (H-Bridge) Inverter",
                    "European IEC 60617 Standard Schematic • 4-Quadrant Bipolar/Unipolar PWM Inverter",
                    "\n".join(elems))

# ==============================================================================
# 9. Three-Phase Inverter
# ==============================================================================
def gen_three_phase_inverter():
    elems = [
        # DC Rails: +Vdc (y=70), 0V (y=410)
        wire(150, 70, 640, 70, stroke="#dc2626"),
        '<circle cx="150" cy="70" r="4.5" fill="#dc2626" />',
        '<text x="135" y="75" class="tag-pos-end">+Vdc</text>',

        wire(150, 410, 640, 410, stroke="#2563eb"),
        '<circle cx="150" cy="410" r="4.5" fill="#2563eb" />',
        '<text x="135" y="415" class="tag-neg-end">0V</text>',

        # DC bus capacitor C_dc at x=200
        junc(200, 70), junc(200, 410),
        cap_v(200, 70, 410, "C_dc", "DC Bus Cap", side="left"),
        gnd(200, 410, "GND"),

        # Leg A at x=320:
        junc(320, 70), junc(320, 410),
        wire(320, 70, 320, 120),
        switch_v(320, 120, 210, "S_1", "", side="left"),
        wire(320, 210, 320, 240),
        junc(320, 240),
        wire(320, 240, 320, 270),
        switch_v(320, 270, 360, "S_4", "", side="left"),
        wire(320, 360, 320, 410),

        # Leg B at x=480:
        junc(480, 70), junc(480, 410),
        wire(480, 70, 480, 120),
        switch_v(480, 120, 210, "S_3", "", side="left"),
        wire(480, 210, 480, 240),
        junc(480, 240),
        wire(480, 240, 480, 270),
        switch_v(480, 270, 360, "S_6", "", side="left"),
        wire(480, 360, 480, 410),

        # Leg C at x=640:
        junc(640, 70), junc(640, 410),
        wire(640, 70, 640, 120),
        switch_v(640, 120, 210, "S_5", "", side="left"),
        wire(640, 210, 640, 240),
        junc(640, 240),
        wire(640, 240, 640, 270),
        switch_v(640, 270, 360, "S_2", "", side="left"),
        wire(640, 360, 640, 410),

        # Phase U (A): Leg A midpoint (320, 240) stepped up to y=100 (bare wire above S3 and S5)
        wire(320, 240, 360, 240),
        wire(360, 240, 360, 100),
        hop_h(360, 480, 560, 100, stroke="#0284c7"),
        hop_h(560, 640, 800, 100, stroke="#0284c7"),
        terminal(800, 100, "Phase U (A)", side="right"),

        # Phase V (B): Leg B midpoint (480, 240) stepped to y=225 (bare wire above S2/midpoint)
        wire(480, 240, 520, 240),
        wire(520, 240, 520, 225),
        hop_h(520, 640, 800, 225, stroke="#0284c7"),
        terminal(800, 225, "Phase V (B)", side="right"),

        # Phase W (C): Leg C midpoint (640, 255) stepped to y=350 (jogged past S2 diode to x=720)
        wire(640, 240, 640, 255),
        wire(640, 255, 720, 255),
        wire(720, 255, 720, 350),
        wire(720, 350, 800, 350, stroke="#0284c7"),
        terminal(800, 350, "Phase W (C)", side="right"),
    ]
    return svg_wrap("0 0 940 480", "Three-Phase Voltage Source Inverter (2-Level VSI)",
                    "European IEC 60617 Standard Schematic • 6-Switch Bridge for AC Motor Drives & Grid Tie",
                    "\n".join(elems))

# ==============================================================================
# 10. FOC Control Diagram
# ==============================================================================
def gen_foc_diagram():
    elems = [
        # Motor
        f'<circle cx="800" cy="200" r="45" class="block-accent" />',
        f'<text x="800" y="195" class="label" fill="#0284c7">PMSM</text>',
        f'<text x="800" y="215" class="sublabel">Motor</text>',

        # Inverter Block
        f'<rect x="630" y="155" width="85" height="90" class="block" />',
        f'<text x="672" y="195" class="label">3-Phase</text>',
        f'<text x="672" y="215" class="sublabel">Inverter</text>',
        wire(715, 200, 755, 200),
        f'<polygon points="755,200 745,195 745,205" fill="#1e293b" />',

        # SVM Block
        f'<rect x="500" y="165" width="75" height="70" class="block" />',
        f'<text x="537" y="200" class="label">SVM</text>',
        f'<text x="537" y="218" class="sublabel">PWM Gen</text>',
        wire(575, 200, 630, 200),
        f'<polygon points="630,200 620,195 620,205" fill="#1e293b" />',

        # Inverse Park Transform
        f'<rect x="370" y="165" width="80" height="70" class="block" />',
        f'<text x="410" y="195" class="label">dq &#8594; &#945;&#946;</text>',
        f'<text x="410" y="215" class="sublabel">Inv. Park</text>',
        wire(450, 200, 500, 200),
        f'<polygon points="500,200 490,195 490,205" fill="#1e293b" />',

        # Summing comparator d-axis at (200, 155)
        f'<circle cx="200" cy="155" r="12" class="comp-body" />',
        f'<text x="200" y="161" class="label" style="font-size:16px;">&#931;</text>',
        f'<text x="178" y="148" class="label" style="font-size:11px; font-weight:bold;">+</text>',
        f'<text x="210" y="178" class="label" style="font-size:12px; font-weight:bold;">&#8722;</text>',
        wire(212, 155, 260, 155),
        f'<polygon points="260,155 250,150 250,160" fill="#1e293b" />',

        # Current Controller PI (d-axis)
        f'<rect x="260" y="135" width="80" height="40" class="block" />',
        f'<text x="300" y="160" class="label">PI (d-axis)</text>',
        wire(340, 155, 370, 185),

        # Summing comparator q-axis at (200, 225)
        f'<circle cx="200" cy="225" r="12" class="comp-body" />',
        f'<text x="200" y="231" class="label" style="font-size:16px;">&#931;</text>',
        f'<text x="178" y="218" class="label" style="font-size:11px; font-weight:bold;">+</text>',
        f'<text x="210" y="248" class="label" style="font-size:12px; font-weight:bold;">&#8722;</text>',
        wire(212, 225, 260, 225),
        f'<polygon points="260,225 250,220 250,230" fill="#1e293b" />',

        # Current Controller PI (q-axis)
        f'<rect x="260" y="205" width="80" height="40" class="block" />',
        f'<text x="300" y="230" class="label">PI (q-axis)</text>',
        wire(340, 225, 370, 215),

        # Reference inputs (terminals at x=115, labels on left)
        terminal(115, 155, "i_d* = 0", side="left", color="#16a34a"),
        wire(115, 155, 188, 155),

        terminal(115, 225, "i_q* (Torque)", side="left", color="#16a34a"),
        wire(115, 225, 188, 225),

        # Feedback Park Transform
        f'<rect x="370" y="290" width="80" height="60" class="block" />',
        f'<text x="410" y="315" class="label">&#945;&#946; &#8594; dq</text>',
        f'<text x="410" y="335" class="sublabel">Park</text>',

        # Clarke Transform
        f'<rect x="500" y="290" width="80" height="60" class="block" />',
        f'<text x="540" y="315" class="label">abc &#8594; &#945;&#946;</text>',
        f'<text x="540" y="335" class="sublabel">Clarke</text>',
        wire(500, 320, 450, 320),
        f'<polygon points="450,320 460,315 460,325" fill="#1e293b" />',

        # Current measurement feedback wire from inverter-motor link
        wire(735, 200, 735, 320),
        junc(735, 200),
        wire(735, 320, 580, 320),
        f'<polygon points="580,320 590,315 590,325" fill="#1e293b" />',
        f'<text x="650" y="310" class="sublabel">i_a, i_b, i_c</text>',

        # dq feedback loops to comparators:
        # i_d feedback from (370, 305) to d-axis comparator at (200, 155)
        wire(370, 305, 160, 305),
        wire(160, 305, 160, 185),
        wire(160, 185, 200, 185),
        wire(200, 185, 200, 167),
        f'<polygon points="200,167 195,177 205,177" fill="#1e293b" />',

        # i_q feedback from (370, 325) to q-axis comparator at (200, 225)
        wire(370, 325, 200, 325),
        wire(200, 325, 200, 237),
        f'<polygon points="200,237 195,247 205,247" fill="#1e293b" />',
    ]
    return svg_wrap("0 0 920 400", "Field-Oriented Control (FOC) Architecture",
                    "Direct / Quadrature (dq0) Decoupled Vector Control with Space Vector Modulation",
                    "\n".join(elems))

# ==============================================================================
# 11. NPC Inverter Phase Leg
# ==============================================================================
def gen_npc_leg():
    elems = [
        # DC Rails: +Vdc/2 (y=70), Neutral (y=250), -Vdc/2 (y=430)
        wire(140, 70, 560, 70, stroke="#dc2626"),
        '<circle cx="140" cy="70" r="4.5" fill="#dc2626" />',
        '<text x="125" y="75" class="tag-pos-end">+Vdc / 2</text>',

        wire(140, 250, 400, 250, stroke="#64748b"),
        '<circle cx="140" cy="250" r="4.5" fill="#64748b" />',
        '<text x="125" y="255" class="sublabel-end">Neutral (M)</text>',

        wire(140, 430, 560, 430, stroke="#2563eb"),
        '<circle cx="140" cy="430" r="4.5" fill="#2563eb" />',
        '<text x="125" y="435" class="tag-neg-end">&#8722;Vdc / 2</text>',

        # Split capacitors C1 and C2 at x=200
        junc(200, 70), junc(200, 250), junc(200, 430),
        cap_v(200, 70, 250, "C_1", "Upper DC Cap", side="left"),
        cap_v(200, 250, 430, "C_2", "Lower DC Cap", side="left"),

        # Neutral ground reference at x=260 (between caps and diodes)
        gnd(260, 250, "0V (Neutral)"),

        # Neutral wire extending to clamping branch at x=400
        junc(400, 250),

        # Clamping Diodes at x=400:
        # Upper clamp D_c1: from Neutral (250) up to S1/S2 node (160)
        diode_v(400, 160, 250, direction="up", label="D_c1", val="Upper Clamp", side="left"),
        wire(400, 160, 560, 160),

        # Lower clamp D_c2: from S3/S4 node (340) up to Neutral (250)
        diode_v(400, 250, 340, direction="up", label="D_c2", val="Lower Clamp", side="left"),
        wire(400, 340, 560, 340),

        # 4 Switches in Series at x=560:
        junc(560, 70), junc(560, 160), junc(560, 250), junc(560, 340), junc(560, 430),
        switch_v(560, 70, 160, "S_1", "", side="left"),
        switch_v(560, 160, 250, "S_2", "", side="left"),
        switch_v(560, 250, 340, "S_3", "", side="left"),
        switch_v(560, 340, 430, "S_4", "", side="left"),

        # AC Output from midpoint (560, 250) to (780, 250)
        wire(560, 250, 780, 250, stroke="#0284c7", stroke_width=3),
        terminal(780, 250, "AC Output (3-Level)", side="right"),
        '<text x="792" y="270" class="sublabel-start">+Vdc/2, 0, &#8722;Vdc/2</text>',
    ]
    return svg_wrap("0 0 980 500", "Three-Level Neutral-Point-Clamped (NPC) Inverter Leg",
                    "European IEC 60617 Standard Schematic • Reduced dv/dt Stress & 3-Level Voltage Output",
                    "\n".join(elems))

# ==============================================================================
# 12. MMC Topology
# ==============================================================================
def gen_mmc_topology():
    elems = [
        # DC Rails: +Vdc/2 (y=70), -Vdc/2 (y=440)
        wire(140, 70, 800, 70, stroke="#dc2626"),
        '<circle cx="140" cy="70" r="4.5" fill="#dc2626" />',
        '<text x="125" y="75" class="tag-pos-end">+Vdc / 2</text>',

        wire(140, 440, 800, 440, stroke="#2563eb"),
        '<circle cx="140" cy="440" r="4.5" fill="#2563eb" />',
        '<text x="125" y="445" class="tag-neg-end">&#8722;Vdc / 2</text>',

        # Three Phase Legs: A (x=240), B (x=490), C (x=740)
        # Leg A
        junc(240, 70), junc(240, 440),
        wire(240, 70, 240, 90),
        '<rect x="210" y="90" width="60" height="26" class="block-accent" /><text x="240" y="108" class="sublabel">SM 1</text>',
        wire(240, 116, 240, 124),
        '<rect x="210" y="124" width="60" height="26" class="block-accent" /><text x="240" y="142" class="sublabel">SM 2</text>',
        wire(240, 150, 240, 158),
        '<text x="240" y="168" class="label">&#8942;</text>',
        wire(240, 170, 240, 176),
        '<rect x="210" y="176" width="60" height="26" class="block-accent" /><text x="240" y="194" class="sublabel">SM N</text>',
        wire(240, 202, 240, 216),
        ind_v(240, 216, 255, "L_arm", "", side="right"),
        junc(240, 255),
        wire(240, 255, 150, 255),
        terminal(150, 255, "Phase A (U)", side="left"),

        ind_v(240, 255, 294, "L_arm", "", side="right"),
        wire(240, 294, 240, 308),
        '<rect x="210" y="308" width="60" height="26" class="block-accent" /><text x="240" y="326" class="sublabel">SM 1</text>',
        wire(240, 334, 240, 342),
        '<text x="240" y="352" class="label">&#8942;</text>',
        wire(240, 354, 240, 360),
        '<rect x="210" y="360" width="60" height="26" class="block-accent" /><text x="240" y="378" class="sublabel">SM N</text>',
        wire(240, 386, 240, 440),

        # Leg B
        junc(490, 70), junc(490, 440),
        wire(490, 70, 490, 90),
        '<rect x="460" y="90" width="60" height="26" class="block-accent" /><text x="490" y="108" class="sublabel">SM 1</text>',
        wire(490, 116, 490, 124),
        '<rect x="460" y="124" width="60" height="26" class="block-accent" /><text x="490" y="142" class="sublabel">SM 2</text>',
        wire(490, 150, 490, 158),
        '<text x="490" y="168" class="label">&#8942;</text>',
        wire(490, 170, 490, 176),
        '<rect x="460" y="176" width="60" height="26" class="block-accent" /><text x="490" y="194" class="sublabel">SM N</text>',
        wire(490, 202, 490, 216),
        ind_v(490, 216, 255, "L_arm", "", side="left"),
        junc(490, 255),
        wire(490, 255, 590, 255),
        terminal(590, 255, "Phase B (V)", side="right"),

        ind_v(490, 255, 294, "L_arm", "", side="left"),
        wire(490, 294, 490, 308),
        '<rect x="460" y="308" width="60" height="26" class="block-accent" /><text x="490" y="326" class="sublabel">SM 1</text>',
        wire(490, 334, 490, 342),
        '<text x="490" y="352" class="label">&#8942;</text>',
        wire(490, 354, 490, 360),
        '<rect x="460" y="360" width="60" height="26" class="block-accent" /><text x="490" y="378" class="sublabel">SM N</text>',
        wire(490, 386, 490, 440),

        # Leg C
        junc(740, 70), junc(740, 440),
        wire(740, 70, 740, 90),
        '<rect x="710" y="90" width="60" height="26" class="block-accent" /><text x="740" y="108" class="sublabel">SM 1</text>',
        wire(740, 116, 740, 124),
        '<rect x="710" y="124" width="60" height="26" class="block-accent" /><text x="740" y="142" class="sublabel">SM 2</text>',
        wire(740, 150, 740, 158),
        '<text x="740" y="168" class="label">&#8942;</text>',
        wire(740, 170, 740, 176),
        '<rect x="710" y="176" width="60" height="26" class="block-accent" /><text x="740" y="194" class="sublabel">SM N</text>',
        wire(740, 202, 740, 216),
        ind_v(740, 216, 255, "L_arm", "", side="right"),
        junc(740, 255),
        wire(740, 255, 870, 255),
        terminal(870, 255, "Phase C (W)", side="right"),

        ind_v(740, 255, 294, "L_arm", "", side="right"),
        wire(740, 294, 740, 308),
        '<rect x="710" y="308" width="60" height="26" class="block-accent" /><text x="740" y="326" class="sublabel">SM 1</text>',
        wire(740, 334, 740, 342),
        '<text x="740" y="352" class="label">&#8942;</text>',
        wire(740, 354, 740, 360),
        '<rect x="710" y="360" width="60" height="26" class="block-accent" /><text x="740" y="378" class="sublabel">SM N</text>',
        wire(740, 386, 740, 440),
    ]
    return svg_wrap("0 0 1020 520", "Modular Multilevel Converter (MMC) Topology",
                    "European IEC 60617 Standard Schematic • Cascaded Submodule HVDC &amp; STATCOM Architecture",
                    "\n".join(elems))

# ==============================================================================
# 13. MMC Submodules
# ==============================================================================
def gen_mmc_submodules():
    elems = [
        # Half-Bridge Submodule (Left, x=50..430)
        '<rect x="50" y="65" width="390" height="305" class="card" />',
        '<text x="245" y="95" class="label" fill="#0284c7">Half-Bridge Submodule (HBSM)</text>',
        '<text x="245" y="115" class="sublabel">2-Level (0, +V_sm)</text>',

        # HBSM Capacitor at x=130
        junc(130, 150), junc(130, 330),
        cap_v(130, 150, 330, "C_sm", "", side="left"),
        wire(130, 150, 270, 150),
        wire(130, 330, 370, 330),

        # HBSM Switches at x=270: S1 (170 to 230), S2 (250 to 310)
        junc(270, 150), junc(270, 240), junc(270, 330),
        wire(270, 150, 270, 170),
        switch_v(270, 170, 230, "S_1", "", side="left"),
        wire(270, 230, 270, 250),
        switch_v(270, 250, 310, "S_2", "", side="left"),
        wire(270, 310, 270, 330),

        # Terminals: Terminal 1 from (270, 240) to (370, 240); Terminal 2 from bottom rail at y=330
        wire(270, 240, 370, 240),
        terminal(370, 240, "Terminal 1", side="right"),
        terminal(370, 330, "Terminal 2", side="right"),

        # Full-Bridge Submodule (Right, x=480..920)
        '<rect x="480" y="65" width="440" height="305" class="card" />',
        '<text x="700" y="95" class="label" fill="#0284c7">Full-Bridge Submodule (FBSM)</text>',
        '<text x="700" y="115" class="sublabel">3-Level (&#8722;V_sm, 0, +V_sm) • DC Fault Clearing</text>',

        # Upper and lower DC bus inside FBSM at y=150 and y=330
        junc(550, 150), junc(550, 330),
        cap_v(550, 150, 330, "C_sm", "", side="left"),
        wire(550, 150, 770, 150),
        wire(550, 330, 770, 330),

        # Leg 1 at x=640: S1 (170..230), S2 (250..310)
        junc(640, 150), junc(640, 240), junc(640, 330),
        wire(640, 150, 640, 170),
        switch_v(640, 170, 230, "S_1", "", side="left"),
        wire(640, 230, 640, 250),
        switch_v(640, 250, 310, "S_2", "", side="left"),
        wire(640, 310, 640, 330),

        # Leg 2 at x=770: S3 (170..230), S4 (250..310)
        junc(770, 150), junc(770, 240), junc(770, 330),
        wire(770, 150, 770, 170),
        switch_v(770, 170, 230, "S_3", "", side="left"),
        wire(770, 230, 770, 250),
        switch_v(770, 250, 310, "S_4", "", side="left"),
        wire(770, 310, 770, 330),

        # Leg 1 midpoint (640, 240) stepped up to y=160 (bare wire above S3)
        # Hops over Leg 2 at (770, 160)
        wire(640, 240, 670, 240),
        wire(670, 240, 670, 160),
        hop_h(670, 770, 840, 160),
        terminal(840, 160, "Terminal 1", side="right"),

        # Leg 2 midpoint (770, 240) goes straight horizontally out to Terminal 2!
        # Clear space between S3 and S4 diodes!
        wire(770, 240, 840, 240),
        terminal(840, 240, "Terminal 2", side="right"),
    ]
    return svg_wrap("0 0 950 400", "MMC Submodule Configurations (HBSM vs. FBSM)",
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
