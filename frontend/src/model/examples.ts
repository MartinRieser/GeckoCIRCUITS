/**
 * Built-in educational circuit examples and blank template in .ipes ASCII format.
 * Enables 1-click exploration and immediate simulation.
 */

export interface CircuitExample {
  id: string;
  name: string;
  category: string;
  description: string;
  content: string;
}

export const BLANK_CIRCUIT_IPES = `
tDURATION 0.02
dt 1e-06
tPAUSE -1.0
T_pre -1.0
dt_pre 0.0
solverType 0
dpix 16
fontSize 12
fontTyp Dialog
worksheetSize 600_600
FileVersion 1
DtStor 2026-08-16
dataContainerSignals[] V_out
`;

export const RLC_CIRCUIT_IPES = `
verbindungLK (0)
<Connection>
label NIX_NIX_NIX
x 4 8
y 6 6
enabledShorted 0
parentSheetIdentifier 0
connectorType LK
uniqueObjectIdentifier 100
<\\Connection>

verbindungLK (1)
<Connection>
label NIX_NIX_NIX
x 12 16
y 6 6
enabledShorted 0
parentSheetIdentifier 0
connectorType LK
uniqueObjectIdentifier 101
<\\Connection>

verbindungLK (2)
<Connection>
label V_out
x 20 22
y 6 6
enabledShorted 0
parentSheetIdentifier 0
connectorType LK
uniqueObjectIdentifier 102
<\\Connection>

verbindungLK (3)
<Connection>
label GND
x 22 4
y 10 10
enabledShorted 0
parentSheetIdentifier 0
connectorType LK
uniqueObjectIdentifier 103
<\\Connection>

e (0)
<ElementLK>
labelAnfangsKnoten[] NIX_NIX_NIX
labelEndKnoten[] GND
enabledShorted 0
parentSheetIdentifier 0
typ 4
uniqueObjectIdentifier 1
x 4
y 8
parameter[] 401.0 24.0 50.0 0.0 0.0
orientierung 503
idStringDialog V_in
<\\ElementLK>

e (1)
<ElementLK>
labelAnfangsKnoten[] NIX_NIX_NIX
labelEndKnoten[] NIX_NIX_NIX
enabledShorted 0
parentSheetIdentifier 0
typ 1
uniqueObjectIdentifier 2
x 10
y 6
parameter[] 5.0
orientierung 502
idStringDialog R1
<\\ElementLK>

e (2)
<ElementLK>
labelAnfangsKnoten[] NIX_NIX_NIX
labelEndKnoten[] V_out
enabledShorted 0
parentSheetIdentifier 0
typ 2
uniqueObjectIdentifier 3
x 14
y 6
parameter[] 0.001 0.0
orientierung 502
idStringDialog L1
<\\ElementLK>

e (3)
<ElementLK>
labelAnfangsKnoten[] V_out
labelEndKnoten[] GND
enabledShorted 0
parentSheetIdentifier 0
typ 3
uniqueObjectIdentifier 4
x 22
y 8
parameter[] 0.0001 0.0
orientierung 503
idStringDialog C1
<\\ElementLK>

tDURATION 0.02
dt 1e-06
tPAUSE -1.0
T_pre -1.0
dt_pre 0.0
solverType 0
dpix 16
fontSize 12
fontTyp Dialog
worksheetSize 600_600
FileVersion 1
DtStor 2026-08-16
dataContainerSignals[] V_out
`;

export const BUCK_CONVERTER_IPES = `
verbindungLK (0)
<Connection>
label NIX_NIX_NIX
x 4 8
y 6 6
enabledShorted 0
parentSheetIdentifier 0
connectorType LK
uniqueObjectIdentifier 201
<\\Connection>

verbindungLK (1)
<Connection>
label SW_node
x 12 14
y 6 6
enabledShorted 0
parentSheetIdentifier 0
connectorType LK
uniqueObjectIdentifier 202
<\\Connection>

verbindungLK (2)
<Connection>
label SW_node
x 14 16
y 6 6
enabledShorted 0
parentSheetIdentifier 0
connectorType LK
uniqueObjectIdentifier 204
<\\Connection>

verbindungLK (3)
<Connection>
label V_out
x 20 22
y 6 6
enabledShorted 0
parentSheetIdentifier 0
connectorType LK
uniqueObjectIdentifier 205
<\\Connection>

verbindungLK (4)
<Connection>
label V_out
x 22 26
y 6 6
enabledShorted 0
parentSheetIdentifier 0
connectorType LK
uniqueObjectIdentifier 206
<\\Connection>

verbindungLK (5)
<Connection>
label GND
x 26 22
y 10 10
enabledShorted 0
parentSheetIdentifier 0
connectorType LK
uniqueObjectIdentifier 209
<\\Connection>

verbindungLK (6)
<Connection>
label GND
x 22 14
y 10 10
enabledShorted 0
parentSheetIdentifier 0
connectorType LK
uniqueObjectIdentifier 210
<\\Connection>

verbindungLK (7)
<Connection>
label GND
x 14 4
y 10 10
enabledShorted 0
parentSheetIdentifier 0
connectorType LK
uniqueObjectIdentifier 211
<\\Connection>

verbindungCONTROL (0)
<Connection>
label NIX_NIX_NIX
x 10 14
y 14 14
enabledShorted 0
parentSheetIdentifier 0
connectorType CONTROL
uniqueObjectIdentifier 212
<\\Connection>

e (0)
<ElementLK>
labelAnfangsKnoten[] NIX_NIX_NIX
labelEndKnoten[] GND
enabledShorted 0
parentSheetIdentifier 0
typ 4
uniqueObjectIdentifier 10
x 4
y 8
parameter[] 401.0 48.0 50.0 0.0 0.0
orientierung 503
idStringDialog V_dc
<\\ElementLK>

e (1)
<ElementLK>
labelAnfangsKnoten[] NIX_NIX_NIX
labelEndKnoten[] SW_node
enabledShorted 0
parentSheetIdentifier 0
typ 10
uniqueObjectIdentifier 11
x 10
y 6
parameter[] 0.01 1000000.0 0.0 0.0
orientierung 502
idStringDialog IGBT1
<\\ElementLK>

e (2)
<ElementLK>
labelAnfangsKnoten[] SW_node
labelEndKnoten[] GND
enabledShorted 0
parentSheetIdentifier 0
typ 6
uniqueObjectIdentifier 12
x 14
y 8
parameter[] 1000000.0 0.7 0.01 1000000.0
orientierung 501
idStringDialog D_free
<\\ElementLK>

e (3)
<ElementLK>
labelAnfangsKnoten[] SW_node
labelEndKnoten[] V_out
enabledShorted 0
parentSheetIdentifier 0
typ 2
uniqueObjectIdentifier 13
x 18
y 6
parameter[] 0.0001 0.0
orientierung 502
idStringDialog L_filter
<\\ElementLK>

e (4)
<ElementLK>
labelAnfangsKnoten[] V_out
labelEndKnoten[] GND
enabledShorted 0
parentSheetIdentifier 0
typ 3
uniqueObjectIdentifier 14
x 22
y 8
parameter[] 4.7e-05 0.0
orientierung 503
idStringDialog C_filter
<\\ElementLK>

e (5)
<ElementLK>
labelAnfangsKnoten[] V_out
labelEndKnoten[] GND
enabledShorted 0
parentSheetIdentifier 0
typ 1
uniqueObjectIdentifier 15
x 26
y 8
parameter[] 10.0
orientierung 503
idStringDialog R_load
<\\ElementLK>

c (0)
<ElementCONTROL>
labelAnfangsKnoten[] NIX_NIX_NIX
labelEndKnoten[] NIX_NIX_NIX
enabledShorted 0
parentSheetIdentifier 0
typ 4
uniqueObjectIdentifier 16
x 8
y 14
parameter[] 1.0 1.0 20000.0 0.0 0.0 0.45 0 0
orientierung 502
idStringDialog Signal.1
<\\ElementCONTROL>

c (1)
<ElementCONTROL>
labelAnfangsKnoten[] NIX_NIX_NIX
labelEndKnoten[] NIX_NIX_NIX
enabledShorted 0
parentSheetIdentifier 0
typ 6
uniqueObjectIdentifier 17
x 16
y 14
parameter[] 0.0
orientierung 502
idStringDialog Gate.1
<\\ElementCONTROL>

tDURATION 0.005
dt 5e-07
tPAUSE -1.0
T_pre -1.0
dt_pre 0.0
solverType 0
dpix 16
fontSize 12
fontTyp Dialog
worksheetSize 600_600
FileVersion 1
DtStor 2026-08-16
dataContainerSignals[] V_out
`;

export const RC_FILTER_IPES = `
verbindungLK (0)
<Connection>
label NIX_NIX_NIX
x 4 8
y 6 6
enabledShorted 0
parentSheetIdentifier 0
connectorType LK
uniqueObjectIdentifier 301
<\\Connection>

verbindungLK (1)
<Connection>
label V_out
x 12 16
y 6 6
enabledShorted 0
parentSheetIdentifier 0
connectorType LK
uniqueObjectIdentifier 302
<\\Connection>

verbindungLK (2)
<Connection>
label GND
x 16 4
y 10 10
enabledShorted 0
parentSheetIdentifier 0
connectorType LK
uniqueObjectIdentifier 304
<\\Connection>

e (0)
<ElementLK>
labelAnfangsKnoten[] NIX_NIX_NIX
labelEndKnoten[] GND
enabledShorted 0
parentSheetIdentifier 0
typ 4
uniqueObjectIdentifier 31
x 4
y 8
parameter[] 401.0 10.0 50.0 0.0 0.0
orientierung 503
idStringDialog V_step
<\\ElementLK>

e (1)
<ElementLK>
labelAnfangsKnoten[] NIX_NIX_NIX
labelEndKnoten[] V_out
enabledShorted 0
parentSheetIdentifier 0
typ 1
uniqueObjectIdentifier 32
x 10
y 6
parameter[] 1000.0
orientierung 502
idStringDialog R1
<\\ElementLK>

e (2)
<ElementLK>
labelAnfangsKnoten[] V_out
labelEndKnoten[] GND
enabledShorted 0
parentSheetIdentifier 0
typ 3
uniqueObjectIdentifier 33
x 16
y 8
parameter[] 1e-06 0.0
orientierung 503
idStringDialog C1
<\\ElementLK>

tDURATION 0.02
dt 1e-06
tPAUSE -1.0
T_pre -1.0
dt_pre 0.0
solverType 0
dpix 16
fontSize 12
fontTyp Dialog
worksheetSize 600_600
FileVersion 1
DtStor 2026-08-16
dataContainerSignals[] V_out
`;

export const EXAMPLES: CircuitExample[] = [
  {
    id: 'rlc',
    name: 'RLC Resonant Circuit',
    category: 'Analog & Filter',
    description: 'Step response of second-order RLC series resonator with transient oscillation.',
    content: RLC_CIRCUIT_IPES,
  },
  {
    id: 'buck',
    name: 'DC-DC Buck Converter',
    category: 'Power Electronics',
    description: 'Step-down switching converter (48V to ~12V) with freewheeling diode, IGBT switch, gate controller, and LC output filter.',
    content: BUCK_CONVERTER_IPES,
  },
  {
    id: 'rc',
    name: 'RC Low-Pass Filter',
    category: 'Analog & Filter',
    description: 'First-order RC filter demonstrating exponential capacitor charging curve.',
    content: RC_FILTER_IPES,
  },
];
