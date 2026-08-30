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
orientierung 503
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
coupledReferenceID[] 11
x 16
y 14
parameter[] 0.0
orientierung 503
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

export const RC_CLASSIC_IPES = `
verbindungLeistungskreisANZAHL 0
elementANZAHL 3

e (0)
<ElementLK>
labelAnfangsKnoten[] /n1
labelEndKnoten[] /gnd
enabledShorted 1
parentSheetIdentifier 0
typ 4
uniqueObjectIdentifier 100000001
x 7
y 11
parameter[] 401.0 100.0 50.0 0.0 0.0 0.5 0.0 100.0 100.0 0.0 0.0 1.0 1.7976931348623157E308 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog U.1
dxTxt -4.75
dyTxt 1.75

<\\ElementLK>

e (1)
<ElementLK>
labelAnfangsKnoten[] /n1
labelEndKnoten[] /u_out
enabledShorted 1
parentSheetIdentifier 0
typ 1
uniqueObjectIdentifier 100000002
x 13
y 11
parameter[] 10.0 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog R.1
dxTxt -4.375
dyTxt 3.0625

<\\ElementLK>

e (2)
<ElementLK>
labelAnfangsKnoten[] /u_out
labelEndKnoten[] /gnd
enabledShorted 1
parentSheetIdentifier 0
typ 3
uniqueObjectIdentifier 100000003
x 19
y 11
parameter[] 1.0E-4 0.0 0.0 0.0 0.0 0.0 1.0E-4 1.0E-4 0.0 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog C.1
isNonlinear false
dxTxt 3.4375
dyTxt -5.0

<\\ElementLK>

controlANZAHL 2

c (0)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /u_out
enabledShorted 1
parentSheetIdentifier 0
typ 1
uniqueObjectIdentifier 200000001
x 27
y 10
parameter[] 0.0
parameterString[] /u_out/gnd/0
nameOpt[] 
orientierung 503
idStringDialog VOLT.1
shiftLabelsIn[] 
shiftLabelsOut[] true
<detail>
u[] 0.0
<\\detail>
<\\ElementCONTROL>

c (1)
<ElementCONTROL>
labelAnfangsKnoten[] /u_out
labelEndKnoten[] 
enabledShorted 1
parentSheetIdentifier 0
typ 5
uniqueObjectIdentifier 200000002
x 31
y 10
parameter[] 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] 
orientierung 503
idStringDialog SCOPE.1
shiftLabelsIn[] true
<\\ElementCONTROL>

thermANZAHL 0

optimizerName[] 
optimizerValue[] 
<scripterCode>

<\\scripterCode>
<scripterImports>

<\\scripterImports>
<scripterDeclarations>

<\\scripterDeclarations>

GeckoFileManager
<GeckoFileManager>
<\\GeckoFileManager>


DtStor 2026-08-22
tDURATION 5.0E-3
bl 0
dt 1.0E-6
tPAUSE -1.0
T_pre -1.0
dt_pre 1.0E-6
solverType 0
path c:/parity/rc-lowpass.ipes
dpix 16
fontSize 12
fontTyp Dialog.plain
fensterWidth 1047
fensterHeight 637
worksheetSizeX 900
worksheetSizeY 900
ANSICHT_SHOW_LK_NAME true
ANSICHT_SHOW_LK_PARAMETER true
ANSICHT_SHOW_LK_FLOWDIR true
ANSICHT_SHOW_LK_TEXTLINIE true
ANSICHT_SHOW_THERM_NAME true
ANSICHT_SHOW_THERM_PARAMETER true
ANSICHT_SHOW_THERM_FLOWDIR false
ANSICHT_SHOW_THERM_TEXTLINIE true
ANSICHT_SHOW_CONTROL_NAME false
ANSICHT_SHOW_CONTROL_PARAMETER true
ANSICHT_SHOW_CONTROL_TEXTLINIE true
FileVersion 161
UniqueFileId 111000001
dataContainerSignals[] /u_out
======================

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
  {
    id: 'rc-classic',
    name: 'RC Low-Pass (classic file)',
    category: 'Analog & Filter',
    description: 'Genuine classic-authored .ipes - runs bit-identically on the Headless and Classic engines.',
    content: RC_CLASSIC_IPES,
  },
];
