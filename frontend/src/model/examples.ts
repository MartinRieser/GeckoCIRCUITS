/**
 * Built-in educational circuit examples and blank template in .ipes ASCII format.
 * Enables 1-click exploration and immediate simulation with zero component collisions
 * and cleanly routed measurement probes.
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

export const BUCK_CONVERTER_IPES = `
verbindungCONTROL (0)
<Verbindung>
label NIX_NIX_NIX
x[] 8 12 
y[] 28 28 
enabledShorted 1
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungCONTROL (1)
<Verbindung>
label NIX_NIX_NIX
x[] 8 10 10 12 
y[] 33 33 29 29 
enabledShorted 1
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungCONTROL (2)
<Verbindung>
label NIX_NIX_NIX
x[] 20 22 22 24 
y[] 24 24 28 28 
enabledShorted 1
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungCONTROL (3)
<Verbindung>
label NIX_NIX_NIX
x[] 16 18 18 24 
y[] 28 28 29 29 
enabledShorted 1
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungCONTROL (4)
<Verbindung>
label NIX_NIX_NIX
x[] 28 32 
y[] 28 28 
enabledShorted 1
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungCONTROL (5)
<Verbindung>
label NIX_NIX_NIX
x[] 36 42 
y[] 28 28 
enabledShorted 1
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungCONTROL (6)
<Verbindung>
label NIX_NIX_NIX
x[] 46 50 
y[] 28 28 
enabledShorted 1
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungCONTROL (7)
<Verbindung>
label NIX_NIX_NIX
x[] 54 60 
y[] 28 28 
enabledShorted 1
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungCONTROL (8)
<Verbindung>
label NIX_NIX_NIX
x[] 56 56 60 
y[] 33 29 29 
enabledShorted 1
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungCONTROL (9)
<Verbindung>
label NIX_NIX_NIX
x[] 64 68 
y[] 28 28 
enabledShorted 1
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungCONTROL (10)
<Verbindung>
label NIX_NIX_NIX
x[] 8 12 
y[] 38 38 
enabledShorted 1
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungCONTROL (11)
<Verbindung>
label NIX_NIX_NIX
x[] 8 10 10 12 
y[] 43 43 39 39 
enabledShorted 1
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungCONTROL (12)
<Verbindung>
label NIX_NIX_NIX
x[] 20 22 22 24 
y[] 34 34 38 38 
enabledShorted 1
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungCONTROL (13)
<Verbindung>
label NIX_NIX_NIX
x[] 16 18 18 24 
y[] 38 38 39 39 
enabledShorted 1
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungCONTROL (14)
<Verbindung>
label NIX_NIX_NIX
x[] 28 40 40 42 
y[] 38 38 29 29 
enabledShorted 1
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungLK (16)
<Verbindung>
label in_dc
x[] 6 6 
y[] 8 10 
enabledShorted 1
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungLK (17)
<Verbindung>
label in_tot
x[] 6 14 
y[] 4 4 
enabledShorted 1
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungLK (18)
<Verbindung>
label sw_node
x[] 18 22 26 
y[] 4 4 4 
enabledShorted 1
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungLK (19)
<Verbindung>
label sw_node
x[] 22 22 
y[] 4 9 
enabledShorted 1
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungLK (20)
<Verbindung>
label out
x[] 30 36 44 
y[] 4 4 4 
enabledShorted 1
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungLK (21)
<Verbindung>
label out
x[] 36 36 
y[] 4 9 
enabledShorted 1
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungLK (22)
<Verbindung>
label out
x[] 44 44 
y[] 4 9 
enabledShorted 1
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungLK (23)
<Verbindung>
label 0
x[] 6 6 22 36 44 
y[] 14 18 18 18 18 
enabledShorted 1
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungLK (24)
<Verbindung>
label 0
x[] 22 22 
y[] 13 18 
enabledShorted 1
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungLK (25)
<Verbindung>
label 0
x[] 36 36 
y[] 13 18 
enabledShorted 1
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungLK (26)
<Verbindung>
label 0
x[] 44 44 
y[] 13 18 
enabledShorted 1
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

e (27)
<ElementLK>
labelAnfangsKnoten[] /in_tot
labelEndKnoten[] /in_dc
enabledShorted 1
parentSheetIdentifier 0
typ 4
uniqueObjectIdentifier 100
x 6
y 6
parameter[] 402.0 5.0 50.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 5.0 0.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[]
orientierung 503
idStringDialog V_AC

<\\ElementLK>

e (28)
<ElementLK>
labelAnfangsKnoten[] /in_dc
labelEndKnoten[] /0
enabledShorted 1
parentSheetIdentifier 0
typ 4
uniqueObjectIdentifier 101
x 6
y 12
parameter[] 401.0 50.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[]
orientierung 503
idStringDialog V_DC

<\\ElementLK>

e (29)
<ElementLK>
labelAnfangsKnoten[] /in_tot
labelEndKnoten[] /sw_node
enabledShorted 1
parentSheetIdentifier 0
typ 7
uniqueObjectIdentifier 102
x 16
y 4
parameter[] 1.0E7 0.01 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[]
orientierung 502
idStringDialog SW1

<\\ElementLK>

e (30)
<ElementLK>
labelAnfangsKnoten[] /0
labelEndKnoten[] /sw_node
enabledShorted 1
parentSheetIdentifier 0
typ 6
uniqueObjectIdentifier 103
x 22
y 11
parameter[] 1.0E7 0.7 0.01 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[]
orientierung 501
idStringDialog D1

<\\ElementLK>

e (31)
<ElementLK>
labelAnfangsKnoten[] /sw_node
labelEndKnoten[] /out
enabledShorted 1
parentSheetIdentifier 0
typ 2
uniqueObjectIdentifier 104
x 28
y 4
parameter[] 0.001 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[]
orientierung 502
idStringDialog L1

<\\ElementLK>

e (32)
<ElementLK>
labelAnfangsKnoten[] /out
labelEndKnoten[] /0
enabledShorted 1
parentSheetIdentifier 0
typ 3
uniqueObjectIdentifier 105
x 36
y 11
parameter[] 1.0E-4 0.0 0.0 0.0 0.0 0.0 1.0E-4 1.0E-4 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[]
orientierung 503
idStringDialog C1

<\\ElementLK>

e (33)
<ElementLK>
labelAnfangsKnoten[] /out
labelEndKnoten[] /0
enabledShorted 1
parentSheetIdentifier 0
typ 1
uniqueObjectIdentifier 106
x 44
y 11
parameter[] 5.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[]
orientierung 503
idStringDialog R_LOAD

<\\ElementLK>

e (34)
<ElementLK>
labelAnfangsKnoten[] /0
labelEndKnoten[] /0
enabledShorted 1
parentSheetIdentifier 0
typ 31
uniqueObjectIdentifier 107
x 6
y 20
parameter[] 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[]
orientierung 503
idStringDialog GND

<\\ElementLK>

c (0)
<ElementCONTROL>
labelAnfangsKnoten[]
labelEndKnoten[] /v_out
enabledShorted 1
parentSheetIdentifier 0
typ 1001
uniqueObjectIdentifier 2001
coupledReferenceID[] 106 
x 6
y 28
parameter[] 0.0 
parameterString[] /R_LOAD
nameOpt[]
orientierung 503
idStringDialog VM_OUT

<\\ElementCONTROL>

c (1)
<ElementCONTROL>
labelAnfangsKnoten[]
labelEndKnoten[] /v_in
enabledShorted 1
parentSheetIdentifier 0
typ 1001
uniqueObjectIdentifier 2002
x 6
y 38
parameter[] 
parameterString[] //in_tot//0/0
nameOpt[]
orientierung 503
idStringDialog VM_IN

<\\ElementCONTROL>

c (3)
<ElementCONTROL>
labelAnfangsKnoten[] /pwm_gate
labelEndKnoten[]
enabledShorted 1
parentSheetIdentifier 0
typ 1000
uniqueObjectIdentifier 2004
coupledReferenceID[] 102 
x 70
y 28
parameter[] 0.0 
parameterString[] /SW1
nameOpt[]
orientierung 503
idStringDialog GATE_SW1

<\\ElementCONTROL>

c (4)
<ElementCONTROL>
labelAnfangsKnoten[]
labelEndKnoten[]
enabledShorted 1
parentSheetIdentifier 0
typ 1033
uniqueObjectIdentifier 2005
x 14
y 28
parameter[] 0.0 
parameterString[]
nameOpt[]
orientierung 503
idStringDialog SH_ADC

<\\ElementCONTROL>

c (5)
<ElementCONTROL>
labelAnfangsKnoten[]
labelEndKnoten[]
enabledShorted 1
parentSheetIdentifier 0
typ 1004
uniqueObjectIdentifier 2006
x 6
y 33
parameter[] 404.0 1.0 2000.0 0.0 0.0 0.1 
parameterString[]
nameOpt[]
orientierung 503
idStringDialog CLK_FB

<\\ElementCONTROL>

c (6)
<ElementCONTROL>
labelAnfangsKnoten[]
labelEndKnoten[]
enabledShorted 1
parentSheetIdentifier 0
typ 1005
uniqueObjectIdentifier 2007
x 18
y 24
parameter[] 10.0 
parameterString[]
nameOpt[]
orientierung 503
idStringDialog V_REF

<\\ElementCONTROL>

c (7)
<ElementCONTROL>
labelAnfangsKnoten[]
labelEndKnoten[]
enabledShorted 1
parentSheetIdentifier 0
typ 1017
uniqueObjectIdentifier 2008
x 26
y 28
parameter[] 0.0 
parameterString[]
nameOpt[]
orientierung 503
idStringDialog SUB_ERR

<\\ElementCONTROL>

c (8)
<ElementCONTROL>
labelAnfangsKnoten[]
labelEndKnoten[]
enabledShorted 1
parentSheetIdentifier 0
typ 1007
uniqueObjectIdentifier 2009
x 34
y 28
parameter[] 0.01 0.003 
parameterString[]
nameOpt[]
orientierung 503
idStringDialog PI_REG

<\\ElementCONTROL>

c (9)
<ElementCONTROL>
labelAnfangsKnoten[]
labelEndKnoten[]
enabledShorted 1
parentSheetIdentifier 0
typ 1018
uniqueObjectIdentifier 2010
x 44
y 28
parameter[] 0.0 
parameterString[]
nameOpt[]
orientierung 503
idStringDialog ADD_FF

<\\ElementCONTROL>

c (10)
<ElementCONTROL>
labelAnfangsKnoten[]
labelEndKnoten[] /duty
enabledShorted 1
parentSheetIdentifier 0
typ 1021
uniqueObjectIdentifier 2011
x 52
y 28
parameter[] 0.02 0.9 
parameterString[]
nameOpt[]
orientierung 503
idStringDialog LIMIT

<\\ElementCONTROL>

c (11)
<ElementCONTROL>
labelAnfangsKnoten[]
labelEndKnoten[]
enabledShorted 1
parentSheetIdentifier 0
typ 1004
uniqueObjectIdentifier 2012
x 54
y 33
parameter[] 403.0 0.5 20000.0 0.5 0.0 0.5 
parameterString[]
nameOpt[]
orientierung 503
idStringDialog TRI

<\\ElementCONTROL>

c (12)
<ElementCONTROL>
labelAnfangsKnoten[]
labelEndKnoten[]
enabledShorted 1
parentSheetIdentifier 0
typ 1010
uniqueObjectIdentifier 2013
x 62
y 28
parameter[] 0.0 
parameterString[]
nameOpt[]
orientierung 503
idStringDialog CMP

<\\ElementCONTROL>

c (13)
<ElementCONTROL>
labelAnfangsKnoten[]
labelEndKnoten[]
enabledShorted 1
parentSheetIdentifier 0
typ 1033
uniqueObjectIdentifier 2014
x 14
y 38
parameter[] 0.0 
parameterString[]
nameOpt[]
orientierung 503
idStringDialog SH_VIN

<\\ElementCONTROL>

c (14)
<ElementCONTROL>
labelAnfangsKnoten[]
labelEndKnoten[]
enabledShorted 1
parentSheetIdentifier 0
typ 1004
uniqueObjectIdentifier 2015
x 6
y 43
parameter[] 404.0 1.0 2000.0 0.0 0.0 0.1 
parameterString[]
nameOpt[]
orientierung 503
idStringDialog CLK_FF

<\\ElementCONTROL>

c (15)
<ElementCONTROL>
labelAnfangsKnoten[]
labelEndKnoten[]
enabledShorted 1
parentSheetIdentifier 0
typ 1005
uniqueObjectIdentifier 2016
x 18
y 34
parameter[] 10.0 
parameterString[]
nameOpt[]
orientierung 503
idStringDialog V_REF_FF

<\\ElementCONTROL>

c (16)
<ElementCONTROL>
labelAnfangsKnoten[]
labelEndKnoten[]
enabledShorted 1
parentSheetIdentifier 0
typ 1020
uniqueObjectIdentifier 2017
x 26
y 38
parameter[] 0.0 
parameterString[]
nameOpt[]
orientierung 503
idStringDialog DIV_FF

<\\ElementCONTROL>

sp (53)
<ElementSPECIAL>
labelAnfangsKnoten[]
labelEndKnoten[]
enabledShorted 1
parentSheetIdentifier 0
typ 2001
uniqueObjectIdentifier 5001
x 78
y 40
parameter[] 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[]
orientierung 503
idStringDialog INFO
boxWidth 38
boxHeight 30
<markdownText>
# DC-DC Buck Converter with Sampled Control (50V to 10V)

Synchronous-equivalent step-down buck converter stepping **50 V DC** down to **10 V DC** with discrete 2 kHz sampled feedback and feedforward compensation.

### Power Stage & Filter Architecture
- **Input Supply**: 50 V DC nominal supply with 5 V / 50 Hz AC line ripple.
- **Switching Stage**: 20 kHz PWM gate control driving main switch \`SW1\`.
- **Inductor L1**: $200\\text{ µH}$ energy storage choke.
- **Output Capacitor C1**: $100\\text{ µF}$ smoothing capacitor with $2.0\\text{ }\\Omega$ resistive load (5 A nominal).

### Discrete Sampled Control Loop
1. **Feedback ADC**: \`SH_ADC\` samples load voltage at 2 kHz ($T_s = 0.5\\text{ ms}$).
2. **Regulation**: \`SUB_ERR\` subtracts from \`V_REF\` (10 V) into \`PI_REG\` controller.
3. **Feedforward**: \`DIV_FF\` computes $D_{ff} = V_{ref} / V_{in}$ from sampled input \`SH_VIN\`.
4. **Modulation**: \`ADD_FF\` sums feedforward and PI trim into 20 kHz carrier \`CMP\`.

### Monitored Waveforms (Oscilloscope / Osci 1)
- **CH1 (v_sw, v_out, v_in)**: Switching node voltage, regulated 10 V output, and ripple-bearing input bus.
- **CH2 (duty)**: Real-time control duty cycle demonstrating line disturbance rejection.

### Observed Waveforms (Oscilloscope)
- **V(sw_node)**: High-speed pulsed switching node voltage (+50 V / 0 V) (Graph 1).
- **V(out)**: Regulated DC output voltage (+10 V target) (Graph 1).
- **V(in_tot)**: DC bus input voltage with AC ripple (Graph 1).
- **duty**: Real-time closed-loop duty ratio from feedforward PI controller (Graph 2).
<\\markdownText>

<\\ElementSPECIAL>

optimizerName[]
optimizerValue[] null
<scripterCode>

<\\scripterCode>
<scripterImports>

<\\scripterImports>
<scripterDeclarations>
 
<\\scripterDeclarations>
<extraScriptSourceFiles>
 
<\\extraScriptSourceFiles>

DtStor 2026-10-24
tDURATION 0.1
dt 2.0E-6
tPAUSE -1.0
T_pre 0.0
dt_pre 0.0
solverType 0
path 

dpix 16
fontSize 12
fontTyp Arial
fensterWidth -1
fensterHeight -1
worksheetSize 600x600
ANSICHT_SHOW_LK_NAME true
ANSICHT_SHOW_LK_PARAMETER true
ANSICHT_SHOW_LK_FLOWDIR false
ANSICHT_SHOW_LK_TEXTLINIE true
ANSICHT_SHOW_THERM_NAME true
ANSICHT_SHOW_THERM_PARAMETER true
ANSICHT_SHOW_THERM_FLOWDIR false
ANSICHT_SHOW_THERM_TEXTLINIE true
ANSICHT_SHOW_CONTROL_NAME true
ANSICHT_SHOW_CONTROL_PARAMETER true
ANSICHT_SHOW_CONTROL_TEXTLINIE true
FileVersion 175
UniqueFileId 0
dataContainerSignals[] /V(sw_node)/V(out)/V(in_tot)/duty

<graphRegisters>
count 1
<graphRegister>
id osci_1
name Osci 1
numGraphs 2
signals V(sw_node),V(out),V(in_tot),duty
signalGraphs V(sw_node):1,V(out):1,V(in_tot):1,duty:2
hiddenSignals 
yScaleMode auto
cursorsEnabled false
<\\graphRegister>
<\\graphRegisters>

=======================
`;

export const BOOST_CONVERTER_IPES = `
verbindungLeistungskreisANZAHL 8
verbindungLK (0)
<Verbindung>
label in
x[] 6 7 8 9 10 11 12 
y[] 6 6 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (1)
<Verbindung>
label sw_node
x[] 16 17 18 
y[] 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (2)
<Verbindung>
label sw_node
x[] 18 19 20 21 22 
y[] 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (3)
<Verbindung>
label out
x[] 26 27 28 29 30 
y[] 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (4)
<Verbindung>
label out
x[] 30 31 32 33 34 35 36 
y[] 6 6 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (5)
<Verbindung>
label 0
x[] 6 7 8 9 10 11 12 13 14 15 16 17 18 
y[] 10 10 10 10 10 10 10 10 10 10 10 10 10 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (6)
<Verbindung>
label 0
x[] 18 19 20 21 22 23 24 25 26 27 28 29 30 
y[] 10 10 10 10 10 10 10 10 10 10 10 10 10 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (7)
<Verbindung>
label 0
x[] 30 31 32 33 34 35 36 
y[] 10 10 10 10 10 10 10 
enabled true
connectorType 0
<\\Verbindung>

verbindungControlANZAHL 1
verbindungCONTROL (0)
<Verbindung>
label pwm
x[] 10 11 12 13 14 
y[] 16 16 16 16 16 
enabled true
connectorType 1
<\\Verbindung>

elementANZAHL 6

e (0)
<ElementLK>
labelAnfangsKnoten[] /in
labelEndKnoten[] /0
enabledShorted 1
typ 4
uniqueObjectIdentifier 1001
x 6
y 8
parameter[] 401.0 12.0 50.0 0.0 0.0 0.5 0.0 12.0 0.0 -12.0 0.0 1.0 1.7976931348623157E308 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog V_in
<\\ElementLK>

e (1)
<ElementLK>
labelAnfangsKnoten[] /in
labelEndKnoten[] /sw_node
enabledShorted 1
typ 2
uniqueObjectIdentifier 1002
x 14
y 6
parameter[] 2.0E-4 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
idStringDialog L_boost
<\\ElementLK>

e (2)
<ElementLK>
labelAnfangsKnoten[] /sw_node
labelEndKnoten[] /0
enabledShorted 1
typ 7
uniqueObjectIdentifier 1003
x 18
y 8
parameter[] 1.0E7 0.01 1.0E7 0.0 0.0 0.0 3.0E-5 1.5E-5 0.0 0.0 0.0 0.0 1.0
parameterString[] /GATE.1/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog S_boost
coupledReferenceID[] 2002
<Verluste>
verlustTyp 1
rON 0.01
uf 0.0
kON 3.0E-5
kOFF 1.5E-5
uSWnorm 400.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>
<\\ElementLK>

e (3)
<ElementLK>
labelAnfangsKnoten[] /sw_node
labelEndKnoten[] /out
enabledShorted 1
typ 6
uniqueObjectIdentifier 1004
x 24
y 6
parameter[] 0.01 0.0 0.01 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
idStringDialog D_boost
<Verluste>
verlustTyp 1
rON 0.01
uf 0.7
kON 0.0
kOFF 0.0
uSWnorm -1.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>
<\\ElementLK>

e (4)
<ElementLK>
labelAnfangsKnoten[] /out
labelEndKnoten[] /0
enabledShorted 1
typ 3
uniqueObjectIdentifier 1005
x 30
y 8
parameter[] 1.0E-4 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog C_out
<\\ElementLK>

e (5)
<ElementLK>
labelAnfangsKnoten[] /out
labelEndKnoten[] /0
enabledShorted 1
typ 1
uniqueObjectIdentifier 1006
x 36
y 8
parameter[] 20.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog R_load
<\\ElementLK>

controlANZAHL 2

c (0)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /pwm
enabledShorted 1
typ 4
uniqueObjectIdentifier 2001
x 8
y 16
parameter[] 404.0 1.0 50000.0 0.0 0.0 0.5 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog PWM_GEN
<detail>
typQuelle 404
anteilDC 0.0
amplitudeAC 1.0
frequenz 50000.0
tastverhaeltnis 0.5
phase 0.0
datnamXY not_defined
externalDataFileHashValue 0
<\\detail>
<\\ElementCONTROL>

c (1)
<ElementCONTROL>
labelAnfangsKnoten[] /pwm
labelEndKnoten[] 
enabledShorted 1
typ 6
uniqueObjectIdentifier 2002
x 16
y 16
parameter[] 0.0
parameterString[] /S_boost/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog GATE.1
coupledReferenceID[] 1003
copyCoupledReferenceID[] 1003
<\\ElementCONTROL>

sp (0)
<ElementSPECIAL>
labelAnfangsKnoten[] 
labelEndKnoten[] 
enabledShorted 1
typ 2001
uniqueObjectIdentifier 9001
x 44
y 4
parameter[] 
orientierung 501
idStringDialog NOTE.1
boxWidth 36
boxHeight 28
<markdownText>
# DC-DC Boost Converter (Step-Up)

Step-up converter boosting **12 V DC** input to **24 V DC** output at a nominal duty ratio $D = 0.50$.

### Circuit Architecture
- **Energy Storage L.1**: $200\\text{ µH}$ inductor stores magnetic energy during switch ON interval.
- **Switch S.1**: 50 kHz switching with 50% duty ratio.
- **Diode D.1**: Fast freewheeling diode delivers inductor energy into the load when S.1 turns OFF.
- **Output Capacitor C.1**: $100\\text{ µF}$ reservoir filter maintaining steady 24 V DC.
- **Load R_load**: $10.0\\text{ }\\Omega$ ($2.4\\text{ A}$ nominal load).

### Monitored Waveforms (Oscilloscope / Osci 1)
- **CH1 (v_in)**: 12.0 V DC input voltage.
- **CH2 (v_out)**: Stepped-up 24.0 V DC output voltage.
- **CH3 (i_L)**: Inductor charging/discharging triangular current ripple.

### Observed Waveforms (Oscilloscope)
- **V(out)**: Boosted DC output voltage (24 V) across reservoir capacitor (Graph 1).
- **V(in)**: Input DC supply voltage (12 V) (Graph 1).
- **I(L_boost)**: Triangular boost inductor ripple current (Graph 2).
<\\markdownText>
<\\ElementSPECIAL>

tDURATION 0.012
dt 2.0E-7
solverType 0
FileVersion 1
dataContainerSignals[] /V(out)/V(in)/I(L_boost)
<graphRegisters>
count 1
<graphRegister>
id osci_1
name Osci 1
numGraphs 2
signals V(out),V(in),I(L_boost)
signalGraphs V(out):1,V(in):1,I(L_boost):2
hiddenSignals 
yScaleMode auto
cursorsEnabled false
<\\graphRegister>
<\\graphRegisters>
`;

export const RECTIFIER_CIRCUIT_IPES = `
verbindungLK (0)
<Verbindung>
label ac1
x[] 6 7 8 9 10 11 12 13 14 
y[] 10 10 10 10 10 10 10 10 10 
enabledShorted 0
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungLK (1)
<Verbindung>
label ac2
x[] 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 
y[] 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 
enabledShorted 0
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungLK (2)
<Verbindung>
label ac2
x[] 22 22 22 22 22 
y[] 14 13 12 11 10 
enabledShorted 0
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungLK (3)
<Verbindung>
label dc_plus
x[] 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 
y[] 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 
enabledShorted 0
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungLK (4)
<Verbindung>
label 0
x[] 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 
y[] 18 18 18 18 18 18 18 18 18 18 18 18 18 18 18 18 18 18 18 18 18 18 18 
enabledShorted 0
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungLK (5)
<Verbindung>
label dc_plus
x[] 30 30 30 30 30 
y[] 6 7 8 9 10 
enabledShorted 0
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungLK (6)
<Verbindung>
label dc_plus
x[] 36 36 36 36 36 
y[] 6 7 8 9 10 
enabledShorted 0
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungLK (7)
<Verbindung>
label 0
x[] 30 30 30 30 30 
y[] 14 15 16 17 18 
enabledShorted 0
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungLK (8)
<Verbindung>
label 0
x[] 36 36 36 36 36 
y[] 14 15 16 17 18 
enabledShorted 0
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

e (9)
<ElementLK>
labelAnfangsKnoten[] /ac1
labelEndKnoten[] /ac2
enabledShorted 1
parentSheetIdentifier 0
typ 4
uniqueObjectIdentifier 1001
x 6
y 12
parameter[] 402.0 34.0 50.0 0.0 0.0 0.5 0.0 34.0 0.0 -34.0 0.0 1.0 1.7976931348623157E308 0.0 0.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog V_ac

<\\ElementLK>

e (10)
<ElementLK>
labelAnfangsKnoten[] /ac1
labelEndKnoten[] /dc_plus
enabledShorted 1
parentSheetIdentifier 0
typ 6
uniqueObjectIdentifier 1002
x 14
y 8
parameter[] 0.01 0.0 0.01 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 501
idStringDialog D1
<Verluste>
verlustTyp 1
rON 0.01
uf 0.7
kON 0.0
kOFF 0.0
uSWnorm -1.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>

<\\ElementLK>

e (11)
<ElementLK>
labelAnfangsKnoten[] /0
labelEndKnoten[] /ac1
enabledShorted 1
parentSheetIdentifier 0
typ 6
uniqueObjectIdentifier 1003
x 14
y 16
parameter[] 0.01 0.0 0.01 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 501
idStringDialog D2
<Verluste>
verlustTyp 1
rON 0.01
uf 0.7
kON 0.0
kOFF 0.0
uSWnorm -1.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>

<\\ElementLK>

e (12)
<ElementLK>
labelAnfangsKnoten[] /ac2
labelEndKnoten[] /dc_plus
enabledShorted 1
parentSheetIdentifier 0
typ 6
uniqueObjectIdentifier 1004
x 22
y 8
parameter[] 0.01 0.0 0.01 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 501
idStringDialog D3
<Verluste>
verlustTyp 1
rON 0.01
uf 0.7
kON 0.0
kOFF 0.0
uSWnorm -1.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>

<\\ElementLK>

e (13)
<ElementLK>
labelAnfangsKnoten[] /0
labelEndKnoten[] /ac2
enabledShorted 1
parentSheetIdentifier 0
typ 6
uniqueObjectIdentifier 1005
x 22
y 16
parameter[] 0.01 0.0 0.01 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 501
idStringDialog D4
<Verluste>
verlustTyp 1
rON 0.01
uf 0.7
kON 0.0
kOFF 0.0
uSWnorm -1.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>

<\\ElementLK>

e (14)
<ElementLK>
labelAnfangsKnoten[] /dc_plus
labelEndKnoten[] /0
enabledShorted 1
parentSheetIdentifier 0
typ 3
uniqueObjectIdentifier 1006
x 30
y 12
parameter[] 4.7E-4 0.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog C_filter

<\\ElementLK>

e (15)
<ElementLK>
labelAnfangsKnoten[] /dc_plus
labelEndKnoten[] /0
enabledShorted 1
parentSheetIdentifier 0
typ 1
uniqueObjectIdentifier 1007
x 36
y 12
parameter[] 50.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog R_load

<\\ElementLK>

sp (19)
<ElementSPECIAL>
labelAnfangsKnoten[]
labelEndKnoten[]
enabledShorted 1
parentSheetIdentifier 0
typ 2001
uniqueObjectIdentifier 9001
x 44
y 9
parameter[] NaN 
parameterString[]
nameOpt[]
orientierung 501
idStringDialog NOTE.1
boxWidth 36.0
boxHeight 28.0
<markdownText>
# Diode Bridge Rectifier (AC to DC)

Full-wave Graetz diode bridge converting **50 Hz AC mains** into a smoothed **DC bus voltage**.

### Operating Principles
- **AC Mains (V_ac)**: 230 V rms (325 V peak), 50 Hz sinusoidal source.
- **Bridge Diodes (D.1–D.4)**: Alternating pairs conduct during positive and negative AC half-cycles.
- **Filter Capacitor C.1**: $470\\text{ µF}$ reservoir smoothing the 100 Hz rectified voltage ripple.
- **Load Resistor R_load**: $50\\text{ }\\Omega$ DC load drawing ~6 A nominal.

### Monitored Waveforms (Oscilloscope / Osci 1)
- **CH1 (v_ac)**: 50 Hz sinusoidal AC mains voltage.
- **CH2 (v_dc)**: Smoothed DC bus voltage with ripple $\\Delta V_{dc} \\approx \\frac{I_{load}}{2 f C}$.
- **CH3 (i_load)**: Continuous DC output current through \`R_load\`.

### Observed Waveforms (Oscilloscope)
- **V(dc_plus)**: Rectified and smoothed DC bus voltage across reservoir capacitor (Graph 1).
- **V(ac1|ac2)**: AC line sinusoidal input voltage (Graph 1).
- **I(R_load)**: Continuous resistive DC load current (Graph 2).
<\\markdownText>

<\\ElementSPECIAL>

optimizerName[]
optimizerValue[] null
<scripterCode>

<\\scripterCode>
<scripterImports>

<\\scripterImports>
<scripterDeclarations>
 
<\\scripterDeclarations>
<extraScriptSourceFiles>
 
<\\extraScriptSourceFiles>

DtStor 2026-11-07
tDURATION 0.04
dt 2.0E-6
tPAUSE -1.0
T_pre 0.0
dt_pre 2.0E-6
solverType 0
path 

dpix 16
fontSize 12
fontTyp Arial
fensterWidth -1
fensterHeight -1
worksheetSize 600x600
ANSICHT_SHOW_LK_NAME true
ANSICHT_SHOW_LK_PARAMETER true
ANSICHT_SHOW_LK_FLOWDIR false
ANSICHT_SHOW_LK_TEXTLINIE true
ANSICHT_SHOW_THERM_NAME true
ANSICHT_SHOW_THERM_PARAMETER true
ANSICHT_SHOW_THERM_FLOWDIR false
ANSICHT_SHOW_THERM_TEXTLINIE true
ANSICHT_SHOW_CONTROL_NAME true
ANSICHT_SHOW_CONTROL_PARAMETER true
ANSICHT_SHOW_CONTROL_TEXTLINIE true
FileVersion 1
UniqueFileId 0
dataContainerSignals[] /V(dc_plus)/V(ac1|ac2)/I(R_load)
<graphRegisters>
count 1
<graphRegister>
id osci_1
name Osci 1
numGraphs 2
signals V(dc_plus),V(ac1|ac2),I(R_load)
signalGraphs V(dc_plus):1,V(ac1|ac2):1,I(R_load):2
hiddenSignals 
yScaleMode auto
cursorsEnabled false
<\\graphRegister>
<\\graphRegisters>

=======================
`;

export const THREE_SCOPES_RLC_IPES = `
verbindungLeistungskreisANZAHL 4
verbindungLK (0)
<Verbindung>
label V_in
x[] 6 7 8 9 10 11 12 
y[] 6 6 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (1)
<Verbindung>
label n_mid
x[] 16 17 18 19 20 
y[] 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (2)
<Verbindung>
label V_out
x[] 24 25 26 27 28 29 30 
y[] 6 6 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (3)
<Verbindung>
label 0
x[] 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 
y[] 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 
enabled true
connectorType 0
<\\Verbindung>

verbindungControlANZAHL 0
elementANZAHL 4

e (0)
<ElementLK>
labelAnfangsKnoten[] /V_in
labelEndKnoten[] /0
enabledShorted 1
typ 4
uniqueObjectIdentifier 1001
x 6
y 8
parameter[] 401.0 24.0 50.0 0.0 0.0 0.5 0.0 24.0 0.0 -24.0 0.0 1.0 1.7976931348623157E308 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog V_step
<\\ElementLK>

e (1)
<ElementLK>
labelAnfangsKnoten[] /V_in
labelEndKnoten[] /n_mid
enabledShorted 1
typ 1
uniqueObjectIdentifier 1002
x 14
y 6
parameter[] 5.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
idStringDialog R1
<\\ElementLK>

e (2)
<ElementLK>
labelAnfangsKnoten[] /n_mid
labelEndKnoten[] /V_out
enabledShorted 1
typ 2
uniqueObjectIdentifier 1003
x 22
y 6
parameter[] 0.001 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
idStringDialog L1
<\\ElementLK>

e (3)
<ElementLK>
labelAnfangsKnoten[] /V_out
labelEndKnoten[] /0
enabledShorted 1
typ 3
uniqueObjectIdentifier 1004
x 30
y 8
parameter[] 0.00001 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog C1
<\\ElementLK>

controlANZAHL 0

sp (0)
<ElementSPECIAL>
labelAnfangsKnoten[] 
labelEndKnoten[] 
enabledShorted 1
typ 2001
uniqueObjectIdentifier 9001
x 36
y 6
parameter[] 
orientierung 501
idStringDialog NOTE.1
boxWidth 36
boxHeight 28
<markdownText>
# Multi-Instrument RLC Circuit (Osci 1 & Osci 2)

Demonstrates multiple oscilloscope view registers monitoring complementary sections of a second-order series RLC resonator.

### Circuit Topology
- **Step Source (V_in)**: Step voltage from 0 V to 10 V at $t = 0$.
- **Series Resistor R.1**: $2.0\\text{ }\\Omega$ damping resistance.
- **Series Inductor L.1**: $1.0\\text{ mH}$ energy storage.
- **Capacitor C.1**: $10\\text{ µF}$ resonator capacitance.
- Characteristic resonant frequency: $\\omega_0 = \\frac{1}{\\sqrt{LC}} \\approx 10\\text{ krad/s}$ ($f_0 \\approx 1.59\\text{ kHz}$).

### Instrument Assignments
- **Osci 1 (In/Out)**: Global input step (\`v_in\`) vs output capacitor voltage (\`v_out\`).
- **Osci 2 (Component Voltages)**: Differential component voltages (\`v_R1\`, \`v_L1\`, \`v_C1\`) confirming Kirchhoff's Voltage Law: $V_{in}(t) = v_R(t) + v_L(t) + v_C(t)$.

### Observed Waveforms (Oscilloscope)
- **Osci 1 (Step Response)**: Output ringing \`V(V_out)\` and \`V(V_in)\` (Graph 1), resonant current \`I(L1)\` (Graph 2).
- **Osci 2 (Component Voltages)**: Resistor \`V(R1)\` (Graph 1), inductor \`V(L1)\` (Graph 2), capacitor \`V(C1)\` (Graph 3).
<\\markdownText>
<\\ElementSPECIAL>

tDURATION 0.01
dt 1e-06
solverType 0
FileVersion 1
dataContainerSignals[] /V(V_out)/V(V_in)/I(L1)
<graphRegisters>
count 2
<graphRegister>
id osci_1
name Step Response
numGraphs 2
signals V(V_out),V(V_in),I(L1)
signalGraphs V(V_out):1,V(V_in):1,I(L1):2
hiddenSignals 
yScaleMode auto
cursorsEnabled false
<\\graphRegister>
<graphRegister>
id osci_2
name Component Voltages
numGraphs 3
signals V(R1),V(L1),V(C1)
signalGraphs V(R1):1,V(L1):2,V(C1):3
hiddenSignals 
yScaleMode auto
cursorsEnabled false
<\\graphRegister>
<\\graphRegisters>
`;

export const RLC_CIRCUIT_IPES = `
verbindungLeistungskreisANZAHL 4
verbindungLK (0)
<Verbindung>
label in
x[] 6 7 8 9 10 11 12 
y[] 6 6 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (1)
<Verbindung>
label n_mid
x[] 16 17 18 19 20 
y[] 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (2)
<Verbindung>
label out
x[] 24 25 26 27 28 29 30 
y[] 6 6 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (3)
<Verbindung>
label 0
x[] 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 
y[] 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 
enabled true
connectorType 0
<\\Verbindung>

verbindungControlANZAHL 0
elementANZAHL 4

e (0)
<ElementLK>
labelAnfangsKnoten[] /in
labelEndKnoten[] /0
enabledShorted 1
typ 4
uniqueObjectIdentifier 1001
x 6
y 8
parameter[] 401.0 24.0 50.0 0.0 0.0 0.5 0.0 24.0 0.0 -24.0 0.0 1.0 1.7976931348623157E308 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog V_step
<\\ElementLK>

e (1)
<ElementLK>
labelAnfangsKnoten[] /in
labelEndKnoten[] /n_mid
enabledShorted 1
typ 1
uniqueObjectIdentifier 1002
x 14
y 6
parameter[] 2.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
idStringDialog R1
<\\ElementLK>

e (2)
<ElementLK>
labelAnfangsKnoten[] /n_mid
labelEndKnoten[] /out
enabledShorted 1
typ 2
uniqueObjectIdentifier 1003
x 22
y 6
parameter[] 0.0005 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
idStringDialog L1
<\\ElementLK>

e (3)
<ElementLK>
labelAnfangsKnoten[] /out
labelEndKnoten[] /0
enabledShorted 1
typ 3
uniqueObjectIdentifier 1004
x 30
y 8
parameter[] 0.00001 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog C1
<\\ElementLK>

controlANZAHL 0

sp (0)
<ElementSPECIAL>
labelAnfangsKnoten[] 
labelEndKnoten[] 
enabledShorted 1
typ 2001
uniqueObjectIdentifier 9001
x 36
y 6
parameter[] 
orientierung 501
idStringDialog NOTE.1
boxWidth 36
boxHeight 28
<markdownText>
# RLC Resonant Circuit

Second-order series RLC resonator demonstrating underdamped transient step response.

### Parameters & Resonance
- **Step Input**: 24 V DC step excitation.
- **Resistor R.1**: 2.0 Ω series damping.
- **Inductor L.1**: 1.0 mH series inductance.
- **Capacitor C.1**: 10 µF tank capacitance.
- **Damping Factor**: $\\zeta = \\frac{R}{2}\\sqrt{\\frac{C}{L}} \\approx 0.10$ (strongly underdamped).
- **Ringing Frequency**: $f_d \\approx 1.58\\text{ kHz}$.

### Signals Tracked in Oscilloscope (Osci 1)
- **CH1 (v_in)**: 24 V step stimulus.
- **CH2 (v_out)**: Underdamped ringing voltage across \`C.1\` with ~73% overshoot.
- **CH3 (i_L)**: Resonant tank current flowing through \`L.1\`.

### Observed Waveforms (Oscilloscope)
- **V(out)**: Underdamped oscillatory ringing step response across C1 (Graph 1).
- **V(in)**: Input step voltage excitation (Graph 1).
- **I(L1)**: Resonant series inductor current (Graph 2).
<\\markdownText>
<\\ElementSPECIAL>

tDURATION 0.005
dt 1e-06
solverType 0
FileVersion 1
dataContainerSignals[] /V(out)/V(in)/I(L1)
<graphRegisters>
count 1
<graphRegister>
id osci_1
name Osci 1
numGraphs 2
signals V(out),V(in),I(L1)
signalGraphs V(out):1,V(in):1,I(L1):2
hiddenSignals 
yScaleMode auto
cursorsEnabled false
<\\graphRegister>
<\\graphRegisters>
`;

export const RC_FILTER_IPES = `
verbindungLeistungskreisANZAHL 3
verbindungLK (0)
<Verbindung>
label in
x[] 6 7 8 9 10 11 12 
y[] 6 6 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (1)
<Verbindung>
label out
x[] 16 17 18 19 20 21 22 
y[] 6 6 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (2)
<Verbindung>
label 0
x[] 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 
y[] 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 
enabled true
connectorType 0
<\\Verbindung>

verbindungControlANZAHL 0
elementANZAHL 3

e (0)
<ElementLK>
labelAnfangsKnoten[] /in
labelEndKnoten[] /0
enabledShorted 1
typ 4
uniqueObjectIdentifier 1001
x 6
y 8
parameter[] 401.0 10.0 50.0 0.0 0.0 0.5 0.0 10.0 10.0 0.0 0.0 1.0 1.7976931348623157E308 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog V_step
<\\ElementLK>

e (1)
<ElementLK>
labelAnfangsKnoten[] /in
labelEndKnoten[] /out
enabledShorted 1
typ 1
uniqueObjectIdentifier 1002
x 14
y 6
parameter[] 1000.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
idStringDialog R1
<\\ElementLK>

e (2)
<ElementLK>
labelAnfangsKnoten[] /out
labelEndKnoten[] /0
enabledShorted 1
typ 3
uniqueObjectIdentifier 1003
x 22
y 8
parameter[] 1.0E-6 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog C1
<\\ElementLK>

controlANZAHL 0

sp (0)
<ElementSPECIAL>
labelAnfangsKnoten[] 
labelEndKnoten[] 
enabledShorted 1
typ 2001
uniqueObjectIdentifier 9001
x 32
y 6
parameter[] 
orientierung 501
idStringDialog NOTE.1
boxWidth 36
boxHeight 28
<markdownText>
# RC Low-Pass Filter

First-order passive RC filter demonstrating step response and exponential capacitor charging curve.

### Circuit Parameters
- **Input Source (V_step)**: 10 V DC step input.
- **Series Resistor R1**: $1.0\\text{ k}\\Omega$.
- **Shunt Capacitor C1**: $1.0\\text{ µF}$.
- **Time Constant**: $\\tau = R \\cdot C = 1.0\\text{ ms}$.
- **Cutoff Frequency**: $f_c = \\frac{1}{2\\pi R C} \\approx 159.2\\text{ Hz}$.

### Observed Waveforms (Oscilloscope)
- **V(out)**: First-order exponential charging and discharging voltage across C1 (Graph 1).
- **V(in)**: Input pulse excitation voltage (Graph 1).
- **I(C1)**: Capacitor charging and discharging current spikes (Graph 2).
<\\markdownText>
<\\ElementSPECIAL>

tDURATION 0.01
dt 1e-06
solverType 0
FileVersion 1
dataContainerSignals[] /V(out)/V(in)/I(C1)
<graphRegisters>
count 1
<graphRegister>
id osci_1
name Osci 1
numGraphs 2
signals V(out),V(in),I(C1)
signalGraphs V(out):1,V(in):1,I(C1):2
hiddenSignals 
yScaleMode auto
cursorsEnabled false
<\\graphRegister>
<\\graphRegisters>
`;


export const SYNC_BUCK_IPES = `
verbindungLeistungskreisANZAHL 4
verbindungLK (0)
<Verbindung>
label in
x[] 6 7 8 9 10 
y[] 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (1)
<Verbindung>
label sw_node
x[] 14 15 16 17 18 19 20 21 22 23 24 
y[] 6 6 6 6 6 6 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (2)
<Verbindung>
label out
x[] 28 29 30 31 32 33 34 35 36 37 38 39 40 
y[] 6 6 6 6 6 6 6 6 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (3)
<Verbindung>
label 0
x[] 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 
y[] 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 
enabled true
connectorType 0
<\\Verbindung>

verbindungControlANZAHL 2
verbindungCONTROL (0)
<Verbindung>
label pwm_hi
x[] 10 11 12 
y[] 18 18 18 
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (1)
<Verbindung>
label pwm_lo
x[] 10 10 11 12 
y[] 22 23 23 23 
enabled true
connectorType 1
<\\Verbindung>

elementANZAHL 7

e (0)
<ElementLK>
labelAnfangsKnoten[] /in
labelEndKnoten[] /0
enabledShorted 1
typ 4
uniqueObjectIdentifier 1001
x 6
y 8
parameter[] 401.0 24.0 50.0 0.0 0.0 0.5 0.0 24.0 0.0 -24.0 0.0 1.0 1.7976931348623157E308 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog V_in
<\\ElementLK>

e (1)
<ElementLK>
labelAnfangsKnoten[] /in
labelEndKnoten[] /sw_node
enabledShorted 1
typ 7
uniqueObjectIdentifier 1002
x 12
y 6
parameter[] 1.0E7 0.01 1.0E7 0.0 0.0 0.0 3.0E-5 1.5E-5 0.0 0.0 0.0 0.0 1.0
parameterString[] /GATE_HI/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
idStringDialog S_hi
coupledReferenceID[] 2002
<Verluste>
verlustTyp 1
rON 0.01
uf 0.0
kON 3.0E-5
kOFF 1.5E-5
uSWnorm 400.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>
<\\ElementLK>

e (2)
<ElementLK>
labelAnfangsKnoten[] /sw_node
labelEndKnoten[] /0
enabledShorted 1
typ 7
uniqueObjectIdentifier 1003
x 18
y 8
parameter[] 1.0E7 0.01 1.0E7 0.0 0.0 0.0 3.0E-5 1.5E-5 0.0 0.0 0.0 0.0 1.0
parameterString[] /GATE_LO/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog S_lo
coupledReferenceID[] 2004
<Verluste>
verlustTyp 1
rON 0.01
uf 0.0
kON 3.0E-5
kOFF 1.5E-5
uSWnorm 400.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>
<\\ElementLK>

e (3)
<ElementLK>
labelAnfangsKnoten[] /0
labelEndKnoten[] /sw_node
enabledShorted 1
typ 6
uniqueObjectIdentifier 1007
x 22
y 8
parameter[] 0.01 0.7 0.01 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 501
idStringDialog D_body
<\\ElementLK>

e (4)
<ElementLK>
labelAnfangsKnoten[] /sw_node
labelEndKnoten[] /out
enabledShorted 1
typ 2
uniqueObjectIdentifier 1004
x 26
y 6
parameter[] 1.0E-4 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
idStringDialog L.1
<\\ElementLK>

e (5)
<ElementLK>
labelAnfangsKnoten[] /out
labelEndKnoten[] /0
enabledShorted 1
typ 3
uniqueObjectIdentifier 1005
x 34
y 8
parameter[] 4.7E-5 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog C.1
<\\ElementLK>

e (6)
<ElementLK>
labelAnfangsKnoten[] /out
labelEndKnoten[] /0
enabledShorted 1
typ 1
uniqueObjectIdentifier 1006
x 40
y 8
parameter[] 4.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog R_load
<\\ElementLK>

controlANZAHL 4

c (0)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /pwm_hi
enabledShorted 1
typ 4
uniqueObjectIdentifier 2001
x 8
y 18
parameter[] 404.0 1.0 50000.0 0.0 0.0 0.5 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog PWM_HI
<detail>
typQuelle 404
anteilDC 0.0
amplitudeAC 1.0
frequenz 50000.0
tastverhaeltnis 0.5
phase 0.0
datnamXY not_defined
externalDataFileHashValue 0
<\\detail>
<\\ElementCONTROL>

c (1)
<ElementCONTROL>
labelAnfangsKnoten[] /pwm_hi
labelEndKnoten[] 
enabledShorted 1
typ 6
uniqueObjectIdentifier 2002
x 14
y 18
parameter[] 0.0
parameterString[] /S_hi/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog GATE_HI
coupledReferenceID[] 1002
copyCoupledReferenceID[] 1002
<\\ElementCONTROL>

c (2)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /pwm_lo
enabledShorted 1
typ 4
uniqueObjectIdentifier 2003
x 8
y 22
parameter[] 404.0 1.0 50000.0 0.0 3.141592653589793 0.48 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog PWM_LO
<detail>
typQuelle 404
anteilDC 0.0
amplitudeAC 1.0
frequenz 50000.0
tastverhaeltnis 0.48
phase 3.141592653589793
datnamXY not_defined
externalDataFileHashValue 0
<\\detail>
<\\ElementCONTROL>

c (3)
<ElementCONTROL>
labelAnfangsKnoten[] /pwm_lo
labelEndKnoten[] 
enabledShorted 1
typ 6
uniqueObjectIdentifier 2004
x 14
y 23
parameter[] 0.0
parameterString[] /S_lo/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog GATE_LO
coupledReferenceID[] 1003
copyCoupledReferenceID[] 1003
<\\ElementCONTROL>

sp (0)
<ElementSPECIAL>
labelAnfangsKnoten[] 
labelEndKnoten[] 
enabledShorted 1
typ 2001
uniqueObjectIdentifier 9001
x 44
y 4
parameter[] 
orientierung 501
idStringDialog NOTE.1
boxWidth 36
boxHeight 28
<markdownText>
# Synchronous Buck Converter (50 kHz)

High-efficiency step-down converter using an active low-side MOSFET switch instead of a freewheeling diode.

### Circuit Topology
- **Input (V_in)**: 24 V DC bus.
- **High-Side Switch (S_hi)**: Driven at 50 kHz ($D = 0.48$).
- **Low-Side Switch (S_lo)**: Complementary active freewheeling ($1 - D = 0.48$ with dead-band).
- **Body Diodes (D_hi, D_lo)**: Prevent shoot-through and handle dead-time commutation.
- **Output Filter**: $L_1 = 100\\text{ µH}$, $C_1 = 47\\text{ µF}$, $R_{load} = 5.0\\text{ }\\Omega$.

### Signals in Oscilloscope (Osci 1)
- **CH1 (v_out)**: Regulated ~11.5 V DC output voltage.
- **CH2 (v_sw)**: Switch node square wave switching between 0 V and 24 V.
- **CH3 (i_L)**: Inductor current ripple through $L_1$.

### Observed Waveforms (Oscilloscope)
- **V(out)**: Low-ripple smoothed DC output voltage (5.0 V) (Graph 1).
- **V(sw_node)**: 50 kHz complementary MOSFET switching node waveform (Graph 1).
- **I(L.1)**: Filter inductor ripple current (Graph 2).
<\\markdownText>
<\\ElementSPECIAL>

tDURATION 0.001
dt 2.0E-7
solverType 0
FileVersion 1
dataContainerSignals[] /V(out)/V(sw_node)/I(L.1)
<graphRegisters>
count 1
<graphRegister>
id osci_1
name Osci 1
numGraphs 2
signals V(out),V(sw_node),I(L.1)
signalGraphs V(out):1,V(sw_node):1,I(L.1):2
hiddenSignals 
yScaleMode auto
cursorsEnabled false
<\\graphRegister>
<\\graphRegisters>
`;

export const BUCK_BOOST_IPES = `
verbindungLeistungskreisANZAHL 4
verbindungLK (0)
<Verbindung>
label in
x[] 6 7 8 9 10 11 12
y[] 6 6 6 6 6 6 6
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (1)
<Verbindung>
label sw_node
x[] 16 17 18 19 20 21 22
y[] 6 6 6 6 6 6 6
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (2)
<Verbindung>
label v_neg
x[] 26 27 28 29 30 31 32 33 34 35 36
y[] 6 6 6 6 6 6 6 6 6 6 6
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (3)
<Verbindung>
label 0
x[] 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36
y[] 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10
enabled true
connectorType 0
<\\Verbindung>

verbindungControlANZAHL 1
verbindungCONTROL (0)
<Verbindung>
label pwm
x[] 10 11 12 13 14 
y[] 16 16 16 16 16 
enabled true
connectorType 1
<\\Verbindung>

elementANZAHL 6

e (0)
<ElementLK>
labelAnfangsKnoten[] /in
labelEndKnoten[] /0
enabledShorted 1
typ 4
uniqueObjectIdentifier 1001
x 6
y 8
parameter[] 401.0 24.0 50.0 0.0 0.0 0.5 0.0 24.0 0.0 -24.0 0.0 1.0 1.7976931348623157E308 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog V_in
<\\ElementLK>

e (1)
<ElementLK>
labelAnfangsKnoten[] /in
labelEndKnoten[] /sw_node
enabledShorted 1
typ 7
uniqueObjectIdentifier 1002
x 14
y 6
parameter[] 1.0E7 0.01 1.0E7 0.0 0.0 0.0 3.0E-5 1.5E-5 0.0 0.0 0.0 0.0 1.0
parameterString[] /GATE.1/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
idStringDialog S.1
coupledReferenceID[] 2002
<Verluste>
verlustTyp 1
rON 0.01
uf 0.0
kON 3.0E-5
kOFF 1.5E-5
uSWnorm 400.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>
<\\ElementLK>

e (2)
<ElementLK>
labelAnfangsKnoten[] /sw_node
labelEndKnoten[] /0
enabledShorted 1
typ 2
uniqueObjectIdentifier 1003
x 18
y 8
parameter[] 1.5E-4 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog L.1
<\\ElementLK>

e (3)
<ElementLK>
labelAnfangsKnoten[] /v_neg
labelEndKnoten[] /sw_node
enabledShorted 1
typ 6
uniqueObjectIdentifier 1004
x 24
y 6
parameter[] 0.01 0.7 0.01 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 504
idStringDialog D.1
<Verluste>
verlustTyp 1
rON 0.01
uf 0.7
kON 0.0
kOFF 0.0
uSWnorm -1.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>
<\\ElementLK>

e (4)
<ElementLK>
labelAnfangsKnoten[] /v_neg
labelEndKnoten[] /0
enabledShorted 1
typ 3
uniqueObjectIdentifier 1005
x 30
y 8
parameter[] 4.7E-5 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog C.1
<\\ElementLK>

e (5)
<ElementLK>
labelAnfangsKnoten[] /v_neg
labelEndKnoten[] /0
enabledShorted 1
typ 1
uniqueObjectIdentifier 1006
x 36
y 8
parameter[] 8.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog R_load
<\\ElementLK>

controlANZAHL 2

c (0)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /pwm
enabledShorted 1
typ 4
uniqueObjectIdentifier 2001
x 8
y 16
parameter[] 404.0 1.0 50000.0 0.0 0.0 0.40 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog PWM_GEN
<detail>
typQuelle 404
anteilDC 0.0
amplitudeAC 1.0
frequenz 50000.0
tastverhaeltnis 0.40
phase 0.0
datnamXY not_defined
externalDataFileHashValue 0
<\\detail>
<\\ElementCONTROL>

c (1)
<ElementCONTROL>
labelAnfangsKnoten[] /pwm
labelEndKnoten[] 
enabledShorted 1
typ 6
uniqueObjectIdentifier 2002
x 16
y 16
parameter[] 0.0
parameterString[] /S.1/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog GATE.1
coupledReferenceID[] 1002
copyCoupledReferenceID[] 1002
<\\ElementCONTROL>

sp (0)
<ElementSPECIAL>
labelAnfangsKnoten[] 
labelEndKnoten[] 
enabledShorted 1
typ 2001
uniqueObjectIdentifier 9001
x 44
y 4
parameter[] 
orientierung 501
idStringDialog NOTE.1
boxWidth 36
boxHeight 28
<markdownText>
# Inverting Buck-Boost Converter (-16V)

Non-isolated inverting DC-DC topology producing negative output from positive **24 V DC** input at $D = 0.40$.

### Circuit Dynamics
- **Switch Interval ($D \\cdot T$)**: Main switch \`S.1\` conducts; inductor \`L.1\` charges from +24 V rail while diode \`D.1\` is reverse-biased.
- **Discharge Interval ($(1-D) T$)**: Diode \`D.1\` becomes forward-biased, discharging \`L.1\` into \`C.1\` and \`R_load\` with reversed polarity.
- **Filter (L.1, C.1)**: $L_1 = 200\\text{ µH}$, $C_1 = 100\\text{ µF}$.
- **Load R_load**: $10.0\\text{ }\\Omega$ carrying $-1.6\\text{ A}$ load current.

### Monitored Waveforms (Oscilloscope / Osci 1)
- **CH1 (v_in)**: +24.0 V input rail.
- **CH2 (v_out)**: Inverted -16.0 V steady-state output.
- **CH3 (i_L)**: Inductor triangular energy ramp.

### Observed Waveforms (Oscilloscope)
- **V(v_neg)**: Inverted negative DC output voltage (-16 V) across output capacitor (Graph 1).
- **V(in)**: Input DC supply voltage (24 V) (Graph 1).
- **I(L.1)**: Triangular energy storage inductor current (Graph 2).
<\\markdownText>
<\\ElementSPECIAL>

tDURATION 0.005
dt 2.0E-7
solverType 0
FileVersion 1
dataContainerSignals[] /V(v_neg)/V(in)/I(L.1)
<graphRegisters>
count 1
<graphRegister>
id osci_1
name Osci 1
numGraphs 2
signals V(v_neg),V(in),I(L.1)
signalGraphs V(v_neg):1,V(in):1,I(L.1):2
hiddenSignals 
yScaleMode auto
cursorsEnabled false
<\\graphRegister>
<\\graphRegisters>
`;

export const INVERTER_IPES = `
verbindungLeistungskreisANZAHL 8
verbindungLK (0)
<Verbindung>
label dc_pos
x[] 6 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26
y[] 9 7 7 7 7 7 7 7 7 7 7 7 7 7 7 7 7 7 7 7 7 7
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (1)
<Verbindung>
label 0
x[] 6 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26
y[] 13 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (2)
<Verbindung>
label out_a
x[] 16 15 14 13 12 11 10 10 10 10 10 10 10 10 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32
y[] 11 11 11 11 11 11 11 10 9 8 7 6 5 4 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (3)
<Verbindung>
label out_b
x[] 26 25 24 23 22 21 20 20 20 20 20 20 20 20 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 44 44 44 44 44 44
y[] 11 11 11 11 11 11 11 12 13 14 15 16 17 18 19 19 19 19 19 19 19 19 19 19 19 19 19 19 19 19 19 19 19 19 19 19 19 19 19 18 17 16 15 14 13
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (4)
<Verbindung>
label out_b
x[] 38 38 38 38 38 38 38
y[] 19 18 17 16 15 14 13
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (5)
<Verbindung>
label load_p
x[] 36 37 38 39 40 41 42 43 44 44 44 44 44 44 44
y[] 3 3 3 3 3 3 3 3 3 4 5 6 7 8 9
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (6)
<Verbindung>
label load_p
x[] 38 38 38 38 38 38 38
y[] 3 4 5 6 7 8 9
enabled true
connectorType 0
<\\Verbindung>

verbindungControlANZAHL 4
verbindungCONTROL (0)
<Verbindung>
label pwm_a
x[] 10 11 12
y[] 23 23 23
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (1)
<Verbindung>
label pwm_a
x[] 10 10 10 10 11 12 13 14 15 16 17 18 18 18 18
y[] 23 22 21 20 20 20 20 20 20 20 20 20 21 22 23
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (2)
<Verbindung>
label pwm_b
x[] 10 11 11 12 
y[] 27 27 28 28 
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (3)
<Verbindung>
label pwm_b
x[] 10 10 10 10 10 11 12 13 14 15 16 17 18 18 18 18
y[] 27 28 29 30 31 31 31 31 31 31 31 31 31 30 29 28
enabled true
connectorType 1
<\\Verbindung>

elementANZAHL 12

e (0)
<ElementLK>
labelAnfangsKnoten[] /dc_pos
labelEndKnoten[] /0
enabledShorted 1
typ 4
uniqueObjectIdentifier 1001
x 6
y 11
parameter[] 401.0 40.0 50.0 0.0 0.0 0.5 0.0 40.0 0.0 -40.0 0.0 1.0 1.7976931348623157E308 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog V_dc
<\\ElementLK>

e (1)
<ElementLK>
labelAnfangsKnoten[] /dc_pos
labelEndKnoten[] /out_a
enabledShorted 1
typ 7
uniqueObjectIdentifier 1002
x 12
y 9
parameter[] 1.0E7 0.01 1.0E7 0.0 0.0 0.0 3.0E-5 1.5E-5 0.0 0.0 0.0 0.0 1.0
parameterString[] /GATE.1/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog S.1
coupledReferenceID[] 2002
<Verluste>
verlustTyp 1
rON 0.01
uf 0.0
kON 3.0E-5
kOFF 1.5E-5
uSWnorm 400.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>
<\\ElementLK>

e (2)
<ElementLK>
labelAnfangsKnoten[] /out_a
labelEndKnoten[] /0
enabledShorted 1
typ 7
uniqueObjectIdentifier 1003
x 12
y 13
parameter[] 1.0E7 0.01 1.0E7 0.0 0.0 0.0 3.0E-5 1.5E-5 0.0 0.0 0.0 0.0 1.0
parameterString[] /GATE.2/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog S.2
coupledReferenceID[] 2004
<Verluste>
verlustTyp 1
rON 0.01
uf 0.0
kON 3.0E-5
kOFF 1.5E-5
uSWnorm 400.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>
<\\ElementLK>

e (3)
<ElementLK>
labelAnfangsKnoten[] /out_a
labelEndKnoten[] /dc_pos
enabledShorted 1
typ 6
uniqueObjectIdentifier 1004
x 16
y 9
parameter[] 0.01 0.7 0.01 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 501
idStringDialog D.1
<\\ElementLK>

e (4)
<ElementLK>
labelAnfangsKnoten[] /0
labelEndKnoten[] /out_a
enabledShorted 1
typ 6
uniqueObjectIdentifier 1005
x 16
y 13
parameter[] 0.01 0.7 0.01 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 501
idStringDialog D.2
<\\ElementLK>

e (5)
<ElementLK>
labelAnfangsKnoten[] /dc_pos
labelEndKnoten[] /out_b
enabledShorted 1
typ 7
uniqueObjectIdentifier 1006
x 22
y 9
parameter[] 1.0E7 0.01 1.0E7 0.0 0.0 0.0 3.0E-5 1.5E-5 0.0 0.0 0.0 0.0 1.0
parameterString[] /GATE.3/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog S.3
coupledReferenceID[] 2009
<Verluste>
verlustTyp 1
rON 0.01
uf 0.0
kON 3.0E-5
kOFF 1.5E-5
uSWnorm 400.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>
<\\ElementLK>

e (6)
<ElementLK>
labelAnfangsKnoten[] /out_b
labelEndKnoten[] /0
enabledShorted 1
typ 7
uniqueObjectIdentifier 1007
x 22
y 13
parameter[] 1.0E7 0.01 1.0E7 0.0 0.0 0.0 3.0E-5 1.5E-5 0.0 0.0 0.0 0.0 1.0
parameterString[] /GATE.4/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog S.4
coupledReferenceID[] 2008
<Verluste>
verlustTyp 1
rON 0.01
uf 0.0
kON 3.0E-5
kOFF 1.5E-5
uSWnorm 400.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>
<\\ElementLK>

e (7)
<ElementLK>
labelAnfangsKnoten[] /out_b
labelEndKnoten[] /dc_pos
enabledShorted 1
typ 6
uniqueObjectIdentifier 1008
x 26
y 9
parameter[] 0.01 0.7 0.01 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 501
idStringDialog D.3
<\\ElementLK>

e (8)
<ElementLK>
labelAnfangsKnoten[] /0
labelEndKnoten[] /out_b
enabledShorted 1
typ 6
uniqueObjectIdentifier 1009
x 26
y 13
parameter[] 0.01 0.7 0.01 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 501
idStringDialog D.4
<\\ElementLK>

e (9)
<ElementLK>
labelAnfangsKnoten[] /out_a
labelEndKnoten[] /load_p
enabledShorted 1
typ 2
uniqueObjectIdentifier 1010
x 34
y 3
parameter[] 0.001 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
idStringDialog L_filter
<\\ElementLK>

e (10)
<ElementLK>
labelAnfangsKnoten[] /load_p
labelEndKnoten[] /out_b
enabledShorted 1
typ 3
uniqueObjectIdentifier 1011
x 38
y 11
parameter[] 2.0E-5 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog C_filter
<\\ElementLK>

e (11)
<ElementLK>
labelAnfangsKnoten[] /load_p
labelEndKnoten[] /out_b
enabledShorted 1
typ 1
uniqueObjectIdentifier 1012
x 44
y 11
parameter[] 10.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog R_load
<\\ElementLK>

controlANZAHL 6

c (0)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /pwm_a
enabledShorted 1
typ 4
uniqueObjectIdentifier 2001
x 8
y 23
parameter[] 404.0 1.0 50.0 0.0 0.0 0.48 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog PWM_A
<detail>
typQuelle 404
anteilDC 0.0
amplitudeAC 1.0
frequenz 50.0
tastverhaeltnis 0.48
phase 0.0
datnamXY not_defined
externalDataFileHashValue 0
<\\detail>
<\\ElementCONTROL>

c (1)
<ElementCONTROL>
labelAnfangsKnoten[] /pwm_a
labelEndKnoten[] 
enabledShorted 1
typ 6
uniqueObjectIdentifier 2002
x 14
y 23
parameter[] 0.0
parameterString[] /S.1/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog GATE.1
coupledReferenceID[] 1002
copyCoupledReferenceID[] 1002
<\\ElementCONTROL>

c (2)
<ElementCONTROL>
labelAnfangsKnoten[] /pwm_a
labelEndKnoten[] 
enabledShorted 1
typ 6
uniqueObjectIdentifier 2008
x 20
y 23
parameter[] 0.0
parameterString[] /S.4/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog GATE.4
coupledReferenceID[] 1007
copyCoupledReferenceID[] 1007
<\\ElementCONTROL>

c (3)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /pwm_b
enabledShorted 1
typ 4
uniqueObjectIdentifier 2003
x 8
y 27
parameter[] 404.0 1.0 50.0 0.0 3.141592653589793 0.48 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog PWM_B
<detail>
typQuelle 404
anteilDC 0.0
amplitudeAC 1.0
frequenz 50.0
tastverhaeltnis 0.48
phase 3.141592653589793
datnamXY not_defined
externalDataFileHashValue 0
<\\detail>
<\\ElementCONTROL>

c (4)
<ElementCONTROL>
labelAnfangsKnoten[] /pwm_b
labelEndKnoten[] 
enabledShorted 1
typ 6
uniqueObjectIdentifier 2004
x 14
y 28
parameter[] 0.0
parameterString[] /S.2/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog GATE.2
coupledReferenceID[] 1003
copyCoupledReferenceID[] 1003
<\\ElementCONTROL>

c (5)
<ElementCONTROL>
labelAnfangsKnoten[] /pwm_b
labelEndKnoten[] 
enabledShorted 1
typ 6
uniqueObjectIdentifier 2009
x 20
y 28
parameter[] 0.0
parameterString[] /S.3/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog GATE.3
coupledReferenceID[] 1006
copyCoupledReferenceID[] 1006
<\\ElementCONTROL>

sp (0)
<ElementSPECIAL>
labelAnfangsKnoten[] 
labelEndKnoten[] 
enabledShorted 1
typ 2001
uniqueObjectIdentifier 9001
x 52
y 4
parameter[] 
orientierung 501
idStringDialog NOTE.1
boxWidth 36
boxHeight 28
<markdownText>
# Single-Phase H-Bridge Inverter (50 Hz)

Full-bridge DC-AC inverter converting **40 V DC** into **50 Hz sinusoidal AC** across a second-order LC low-pass filter.

### Topology & Switching
- **H-Bridge (S.1–S.4)**: Four controlled switches in bridge legs A and B.
- **Modulation**: Complementary 50 Hz square waves driving diagonal pairs (S.1+S.4 vs S.2+S.3).
- **LC Filter**: $L = 1.0\\text{ mH}$, $C = 20\\text{ µF}$ with corner frequency $f_c \\approx 1.1\\text{ kHz}$.
- **AC Load**: $10.0\\text{ }\\Omega$ resistive load.

### Observed Waveforms (Oscilloscope)
- **V(load_p|out_b)**: Filtered 50 Hz AC sinusoidal load voltage (Graph 1).
- **V(out_a|out_b)**: Unfiltered two-level switching voltage (+40 V / -40 V) between bridge legs (Graph 1).
- **I(L_filter)**: Sinusoidal filter inductor current (Graph 2).
<\\markdownText>
<\\ElementSPECIAL>

tDURATION 0.04
dt 2.0E-6
solverType 0
FileVersion 1
dataContainerSignals[] /V(load_p|out_b)/V(out_a|out_b)/I(L_filter)
<graphRegisters>
count 1
<graphRegister>
id osci_1
name Osci 1
numGraphs 2
signals V(load_p|out_b),V(out_a|out_b),I(L_filter)
signalGraphs V(load_p|out_b):1,V(out_a|out_b):1,I(L_filter):2
hiddenSignals 
yScaleMode auto
cursorsEnabled false
<\\graphRegister>
<\\graphRegisters>
`;

export const FLYBACK_IPES = `
verbindungLeistungskreisANZAHL 5
verbindungLK (0)
<Verbindung>
label in
x[] 6 7 8 9 10 
y[] 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (1)
<Verbindung>
label p_sw
x[] 14 15 16 
y[] 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (2)
<Verbindung>
label 0
x[] 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 
y[] 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (3)
<Verbindung>
label s_fly
x[] 22 23 24 
y[] 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (4)
<Verbindung>
label out
x[] 28 29 30 31 32 33 34 35 36 37 38 
y[] 6 6 6 6 6 6 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungControlANZAHL 1
verbindungCONTROL (0)
<Verbindung>
label pwm
x[] 10 11 12 
y[] 16 16 16 
enabled true
connectorType 1
<\\Verbindung>

elementANZAHL 8

e (0)
<ElementLK>
labelAnfangsKnoten[] /in
labelEndKnoten[] /0
enabledShorted 1
typ 4
uniqueObjectIdentifier 1001
x 6
y 8
parameter[] 401.0 24.0 50.0 0.0 0.0 0.5 0.0 24.0 0.0 -24.0 0.0 1.0 1.7976931348623157E308 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog V_in
<\\ElementLK>

e (1)
<ElementLK>
labelAnfangsKnoten[] /in
labelEndKnoten[] /p_sw
enabledShorted 1
typ 7
uniqueObjectIdentifier 1002
x 12
y 6
parameter[] 1.0E7 0.01 1.0E7 0.0 0.0 0.0 3.0E-5 1.5E-5 0.0 0.0 0.0 0.0 1.0
parameterString[] /GATE.1/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
idStringDialog S.1
coupledReferenceID[] 2002
<Verluste>
verlustTyp 1
rON 0.01
uf 0.0
kON 3.0E-5
kOFF 1.5E-5
uSWnorm 400.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>
<\\ElementLK>

e (2)
<ElementLK>
labelAnfangsKnoten[] /p_sw
labelEndKnoten[] /0
enabledShorted 1
typ 12
uniqueObjectIdentifier 1003
x 16
y 8
parameter[] 1.0E-4 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog L_p
<\\ElementLK>

e (3)
<ElementLK>
labelAnfangsKnoten[] /0
labelEndKnoten[] /s_fly
enabledShorted 1
typ 12
uniqueObjectIdentifier 1004
x 22
y 8
parameter[] 1.0E-4 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 501
idStringDialog L_s
<\\ElementLK>

e (4)
<ElementLK>
labelAnfangsKnoten[] /NIX_NIX_NIX
labelEndKnoten[] /NIX_NIX_NIX
enabledShorted 1
typ 9
uniqueObjectIdentifier 1005
x 19
y 5
parameter[] 0.999 1003.0 1004.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
idStringDialog K_coupler
<\\ElementLK>

e (5)
<ElementLK>
labelAnfangsKnoten[] /s_fly
labelEndKnoten[] /out
enabledShorted 1
typ 6
uniqueObjectIdentifier 1006
x 26
y 6
parameter[] 0.01 0.7 0.01 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
idStringDialog D_sec
<\\ElementLK>

e (6)
<ElementLK>
labelAnfangsKnoten[] /out
labelEndKnoten[] /0
enabledShorted 1
typ 3
uniqueObjectIdentifier 1007
x 32
y 8
parameter[] 1.0E-4 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog C_out
<\\ElementLK>

e (7)
<ElementLK>
labelAnfangsKnoten[] /out
labelEndKnoten[] /0
enabledShorted 1
typ 1
uniqueObjectIdentifier 1008
x 38
y 8
parameter[] 10.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog R_load
<\\ElementLK>

controlANZAHL 2

c (0)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /pwm
enabledShorted 1
typ 4
uniqueObjectIdentifier 2001
x 8
y 16
parameter[] 404.0 1.0 50000.0 0.0 0.0 0.40 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog PWM_GEN
<detail>
typQuelle 404
anteilDC 0.0
amplitudeAC 1.0
frequenz 50000.0
tastverhaeltnis 0.40
phase 0.0
datnamXY not_defined
externalDataFileHashValue 0
<\\detail>
<\\ElementCONTROL>

c (1)
<ElementCONTROL>
labelAnfangsKnoten[] /pwm
labelEndKnoten[] 
enabledShorted 1
typ 6
uniqueObjectIdentifier 2002
x 14
y 16
parameter[] 0.0
parameterString[] /S.1/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog GATE.1
coupledReferenceID[] 1002
copyCoupledReferenceID[] 1002
<\\ElementCONTROL>

sp (0)
<ElementSPECIAL>
labelAnfangsKnoten[] 
labelEndKnoten[] 
enabledShorted 1
typ 2001
uniqueObjectIdentifier 9001
x 46
y 4
parameter[] 
orientierung 501
idStringDialog NOTE.1
boxWidth 36
boxHeight 28
<markdownText>
# Flyback Isolated DC-DC Converter

Galvanically isolated buck-boost topology using magnetically coupled inductors ($k = 0.999$).

### Topology & Operation
- **Primary Switch S.1**: High-side 50 kHz switch charging primary magnetizing inductance $L_p = 250\\text{ µH}$.
- **Coupled Inductor K.1**: Magnetic coupling ($k = 0.999$) between $L_p$ and secondary $L_s = 250\\text{ µH}$ ($1:1$ turns ratio).
- **Secondary Diode D.1**: Conduction occurs only when primary switch S.1 turns OFF, dumping stored magnetic flux into output capacitor $C_1 = 100\\text{ µF}$.
- **Load R_load**: $10.0\\text{ }\\Omega$ isolated load.

### Recorded Signals in Oscilloscope (Osci 1)
- **CH1 (v_out)**: Isolated DC output voltage.
- **CH2 (v_sw)**: Primary switch drain node displaying flyback inductive voltage spike.

### Observed Waveforms (Oscilloscope)
- **V(out)**: Isolated secondary DC output voltage (+15 V) (Graph 1).
- **V(p_sw)**: Primary MOSFET drain-source voltage showing flyback spike and plateau (Graph 1).
- **I(L_p)**: Primary transformer winding magnetizing current (Graph 2).
<\\markdownText>
<\\ElementSPECIAL>

tDURATION 0.005
dt 2.0E-7
solverType 0
FileVersion 1
dataContainerSignals[] /V(out)/V(p_sw)/I(L_p)
<graphRegisters>
count 1
<graphRegister>
id osci_1
name Osci 1
numGraphs 2
signals V(out),V(p_sw),I(L_p)
signalGraphs V(out):1,V(p_sw):1,I(L_p):2
hiddenSignals 
yScaleMode auto
cursorsEnabled false
<\\graphRegister>
<\\graphRegisters>
`;

export const CLOSED_LOOP_BUCK_IPES = `
verbindungLeistungskreisANZAHL 4
verbindungLK (0)
<Verbindung>
label in
x[] 6 7 8 9 10
y[] 6 6 6 6 6
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (1)
<Verbindung>
label sw_node
x[] 14 15 16 17 18 19 20
y[] 6 6 6 6 6 6 6
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (2)
<Verbindung>
label out
x[] 24 25 26 27 28 29 30 31 32 33 34
y[] 6 6 6 6 6 6 6 6 6 6 6
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (3)
<Verbindung>
label 0
x[] 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34
y[] 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10
enabled true
connectorType 0
<\\Verbindung>

verbindungControlANZAHL 2
verbindungCONTROL (0)
<Verbindung>
label v_out_scope
x[] 10 11 12 13 14
y[] 16 16 16 16 16
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (1)
<Verbindung>
label pwm
x[] 18 19 20 21 22 
y[] 16 16 16 16 16 
enabled true
connectorType 1
<\\Verbindung>

elementANZAHL 6

e (0)
<ElementLK>
labelAnfangsKnoten[] /in
labelEndKnoten[] /0
enabledShorted 1
typ 4
uniqueObjectIdentifier 1001
x 6
y 8
parameter[] 401.0 24.0 50.0 0.0 0.0 0.5 0.0 24.0 0.0 -24.0 0.0 1.0 1.7976931348623157E308 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog V_in
<\\ElementLK>

e (1)
<ElementLK>
labelAnfangsKnoten[] /in
labelEndKnoten[] /sw_node
enabledShorted 1
typ 7
uniqueObjectIdentifier 1002
x 12
y 6
parameter[] 1.0E7 0.01 1.0E7 0.0 0.0 0.0 3.0E-5 1.5E-5 0.0 0.0 0.0 0.0 1.0
parameterString[] /GATE.1/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
idStringDialog S.1
coupledReferenceID[] 2003
<Verluste>
verlustTyp 1
rON 0.01
uf 0.0
kON 3.0E-5
kOFF 1.5E-5
uSWnorm 400.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>
<\\ElementLK>

e (2)
<ElementLK>
labelAnfangsKnoten[] /0
labelEndKnoten[] /sw_node
enabledShorted 1
typ 6
uniqueObjectIdentifier 1003
x 16
y 8
parameter[] 0.01 0.7 0.01 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 501
idStringDialog D.1
<\\ElementLK>

e (3)
<ElementLK>
labelAnfangsKnoten[] /sw_node
labelEndKnoten[] /out
enabledShorted 1
typ 2
uniqueObjectIdentifier 1004
x 22
y 6
parameter[] 1.0E-4 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
idStringDialog L.1
<\\ElementLK>

e (4)
<ElementLK>
labelAnfangsKnoten[] /out
labelEndKnoten[] /0
enabledShorted 1
typ 3
uniqueObjectIdentifier 1005
x 28
y 8
parameter[] 4.7E-5 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog C.1
<\\ElementLK>

e (5)
<ElementLK>
labelAnfangsKnoten[] /out
labelEndKnoten[] /0
enabledShorted 1
typ 1
uniqueObjectIdentifier 1006
x 34
y 8
parameter[] 4.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog R_load
<\\ElementLK>

controlANZAHL 3

c (0)
<ElementCONTROL>
labelAnfangsKnoten[]
labelEndKnoten[] /v_out_scope
enabledShorted 1
typ 1
uniqueObjectIdentifier 2001
x 8
y 16
parameter[] 0.0
parameterString[] /out/0/0
nameOpt[] null
orientierung 503
idStringDialog VOLT_OUT
<\\ElementCONTROL>

c (1)
<ElementCONTROL>
labelAnfangsKnoten[] /v_out_scope
labelEndKnoten[] /pwm
enabledShorted 1
typ 61
uniqueObjectIdentifier 2002
x 16
y 16
parameter[] 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
orientierung 503
idStringDialog CTRL_MCU
anzXIN 1
anzYOUT 1
showName true
<sourceCode>
double v_ref = 12.0;
double v_fb = xIN[0];
double error = v_ref - v_fb;
v_int = v_int + error * dt * 250.0;
if (v_int > 0.85) v_int = 0.85;
if (v_int < 0.05) v_int = 0.05;
double duty = 0.03 * error + v_int;
if (duty > 0.85) duty = 0.85;
if (duty < 0.05) duty = 0.05;

double f_sw = 50000.0;
double T_sw = 1.0 / f_sw;
double t_cycle = t % T_sw;
if (t_cycle < duty * T_sw) {
    yOUT[0] = 1.0;
} else {
    yOUT[0] = 0.0;
}
return yOUT;
<\\sourceCode>
<staticCode>
<\\staticCode>
<importCode>
<\\importCode>
<staticVariables>
double v_int = 0.50;
<\\staticVariables>
<\\ElementCONTROL>

c (2)
<ElementCONTROL>
labelAnfangsKnoten[] /pwm
labelEndKnoten[] 
enabledShorted 1
typ 6
uniqueObjectIdentifier 2003
x 24
y 16
parameter[] 0.0
parameterString[] /S.1/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog GATE.1
coupledReferenceID[] 1002
copyCoupledReferenceID[] 1002
<\\ElementCONTROL>

sp (0)
<ElementSPECIAL>
labelAnfangsKnoten[] 
labelEndKnoten[] 
enabledShorted 1
typ 2001
uniqueObjectIdentifier 9001
x 44
y 4
parameter[] 
orientierung 501
idStringDialog NOTE.1
boxWidth 36
boxHeight 28
<markdownText>
# Closed-Loop Regulated Buck (PI Control)

Voltage-mode closed-loop DC-DC converter featuring an embedded microcontroller **PI controller script block** regulating output voltage to **12.0 V DC**.

### Control Loop Architecture
- **Reference Target**: Desired voltage $V_{ref} = 12.0\\text{ V}$.
- **Feedback Sensor**: \`VOLT_OUT\` samples output capacitor voltage.
- **Microcontroller Script (CTRL_MCU)**:
  - Calculates error $e(t) = V_{ref} - v_{meas}(t)$.
  - Executes discrete proportional-integral regulator: $u(t) = K_p \\cdot e(t) + K_i \\int e \\, dt$.
  - Generates PWM duty cycle clamped between 5% and 85%.
- **Gate Driver (GATE.1)**: Drives power MOSFET switch \`S.1\` at 50 kHz.

### Oscilloscope Verification (Osci 1)
- Shows fast closed-loop step tracking with minimal overshoot and zero steady-state error.

### Observed Waveforms (Oscilloscope)
- **V(out)**: Regulated DC output voltage (+12.0 V) (Graph 1).
- **v_out_scope**: Feedback sensor signal to microcontroller (Graph 1).
- **I(L.1)**: Continuous filter inductor ripple current (Graph 2).
- **pwm**: Embedded PI controller 50 kHz PWM gate drive (Graph 2).
<\\markdownText>
<\\ElementSPECIAL>

tDURATION 0.003
dt 2.0E-7
solverType 0
FileVersion 1
dataContainerSignals[] /V(out)/v_out_scope/I(L.1)/pwm
<graphRegisters>
count 1
<graphRegister>
id osci_1
name Osci 1
numGraphs 2
signals V(out),v_out_scope,I(L.1),pwm
signalGraphs V(out):1,v_out_scope:1,I(L.1):2,pwm:2
hiddenSignals 
yScaleMode auto
cursorsEnabled false
<\\graphRegister>
<\\graphRegisters>
`;

export const PFC_BOOST_IPES = `
verbindungLeistungskreisANZAHL 10
verbindungLK (0)
<Verbindung>
label ac1
x[] 2 3 4 5 6 7 8 8 8 8
y[] 7 7 7 7 7 7 7 8 9 10
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (1)
<Verbindung>
label ac2
x[] 2 3 4 5 6 7 8 9 10 11 12 13 14 14 14 14
y[] 11 11 11 11 11 11 11 11 11 11 11 11 11 10 9 8
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (2)
<Verbindung>
label rect_pos
x[] 8 9 10 11 12 13 14 15 16 17 18
y[] 4 4 4 4 4 4 4 4 4 4 4
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (3)
<Verbindung>
label 0
x[] 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42
y[] 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (4)
<Verbindung>
label sw_node
x[] 22 23 24 25 26 27 28
y[] 4 4 4 4 4 4 4
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (5)
<Verbindung>
label sw_node
x[] 24 24 24 24
y[] 4 5 6 7
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (6)
<Verbindung>
label 0
x[] 24 24 24 24
y[] 11 12 13 14
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (7)
<Verbindung>
label 0
x[] 36 36 36 36
y[] 11 12 13 14
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (8)
<Verbindung>
label 0
x[] 42 42 42 42
y[] 11 12 13 14
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (9)
<Verbindung>
label v_dc
x[] 32 33 34 35 36 37 38 39 40 41 42
y[] 4 4 4 4 4 4 4 4 4 4 4
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (10)
<Verbindung>
label v_dc
x[] 36 36 36 36
y[] 4 5 6 7
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (11)
<Verbindung>
label v_dc
x[] 42 42 42 42
y[] 4 5 6 7
enabled true
connectorType 0
<\\Verbindung>

elementANZAHL 10

e (0)
<ElementLK>
labelAnfangsKnoten[] /ac1
labelEndKnoten[] /ac2
enabledShorted 1
typ 4
uniqueObjectIdentifier 1001
x 2
y 9
parameter[] 402.0 33.94 50.0 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.0 1.0 1.7976931348623157E308 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog U_grid
<\\ElementLK>

e (1)
<ElementLK>
labelAnfangsKnoten[] /ac1
labelEndKnoten[] /rect_pos
enabledShorted 1
typ 6
uniqueObjectIdentifier 1002
x 8
y 6
parameter[] 0.01 0.7 0.01 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 501
idStringDialog D.1
<\\ElementLK>

e (2)
<ElementLK>
labelAnfangsKnoten[] /ac2
labelEndKnoten[] /rect_pos
enabledShorted 1
typ 6
uniqueObjectIdentifier 1003
x 14
y 6
parameter[] 0.01 0.7 0.01 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 501
idStringDialog D.2
<\\ElementLK>

e (3)
<ElementLK>
labelAnfangsKnoten[] /0
labelEndKnoten[] /ac1
enabledShorted 1
typ 6
uniqueObjectIdentifier 1004
x 8
y 12
parameter[] 0.01 0.7 0.01 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 501
idStringDialog D.3
<\\ElementLK>

e (4)
<ElementLK>
labelAnfangsKnoten[] /0
labelEndKnoten[] /ac2
enabledShorted 1
typ 6
uniqueObjectIdentifier 1005
x 14
y 12
parameter[] 0.01 0.7 0.01 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 501
idStringDialog D.4
<\\ElementLK>

e (5)
<ElementLK>
labelAnfangsKnoten[] /rect_pos
labelEndKnoten[] /sw_node
enabledShorted 1
typ 2
uniqueObjectIdentifier 1006
x 20
y 4
parameter[] 8.0E-4 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
idStringDialog L_boost
<\\ElementLK>

e (6)
<ElementLK>
labelAnfangsKnoten[] /sw_node
labelEndKnoten[] /0
enabledShorted 1
typ 7
uniqueObjectIdentifier 1007
x 24
y 9
parameter[] 1.0E7 0.01 1.0E7 0.0 0.0 0.0 3.0E-5 1.5E-5 0.0 0.0 0.0 0.0 1.0
parameterString[] /GATE.1/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog S.1
coupledReferenceID[] 2003
<Verluste>
verlustTyp 1
rON 0.01
uf 0.0
kON 3.0E-5
kOFF 1.5E-5
uSWnorm 400.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>
<\\ElementLK>

e (7)
<ElementLK>
labelAnfangsKnoten[] /sw_node
labelEndKnoten[] /v_dc
enabledShorted 1
typ 6
uniqueObjectIdentifier 1008
x 30
y 4
parameter[] 0.01 0.7 0.01 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
idStringDialog D_boost
<\\ElementLK>

e (8)
<ElementLK>
labelAnfangsKnoten[] /v_dc
labelEndKnoten[] /0
enabledShorted 1
typ 3
uniqueObjectIdentifier 1009
x 36
y 9
parameter[] 4.7E-4 45.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog C_dc
<\\ElementLK>

e (9)
<ElementLK>
labelAnfangsKnoten[] /v_dc
labelEndKnoten[] /0
enabledShorted 1
typ 1
uniqueObjectIdentifier 1010
x 42
y 9
parameter[] 25.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog R_load
<\\ElementLK>

controlANZAHL 4

c (0)
<ElementCONTROL>
labelAnfangsKnoten[]
labelEndKnoten[] /v_dc_meas
enabledShorted 1
typ 1
uniqueObjectIdentifier 2001
x 10
y 20
parameter[] 0.0
parameterString[] /v_dc/0/0
nameOpt[] null
orientierung 503
idStringDialog VOLT_DC
<\\ElementCONTROL>

c (1)
<ElementCONTROL>
labelAnfangsKnoten[]
labelEndKnoten[] /i_l
enabledShorted 1
typ 2
uniqueObjectIdentifier 2005
x 10
y 24
parameter[] 0.0
parameterString[] /L_boost/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog AMP_IL
coupledReferenceID[] 1006
copyCoupledReferenceID[] 1006
<\\ElementCONTROL>

c (2)
<ElementCONTROL>
labelAnfangsKnoten[] /v_dc_meas/i_l
labelEndKnoten[] /pwm
enabledShorted 1
typ 61
uniqueObjectIdentifier 2002
x 20
y 20
parameter[] 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
orientierung 503
idStringDialog CTRL_PFC
anzXIN 2
anzYOUT 1
showName true
<sourceCode>
//  Active-PFC boost control - runs once per simulation step (dt = 1 us)
//
//  Structure (average-current-mode PFC, textbook two-loop control):
//
//    v_dc --> [outer voltage loop: slow integrator] --> i_amp
//             (amplitude of the grid-current reference, ~5.9 A)
//    i_amp * |sin(wt)| --> i_ref  (rectified-sine current template)
//    i_ref, i_l --> [inner current loop: duty feedforward + PI] --> duty
//    duty --> [50 kHz PWM comparator] --> gate signal for S.1
//
//  The rectified-sine template forces the mains current to follow the
//  shape of the grid voltage, so the converter draws its current in
//  phase with the mains (power factor near 1).

// ---- parameters ------------------------------------------------------
double v_ref = 50.0;               // DC bus setpoint [V]
double v_pk  = 33.94;              // grid peak voltage [V] (24 Vrms * sqrt(2))
double f_sw  = 50000.0;            // PWM switching frequency [Hz]
double omega = 2.0 * PI * 50.0;    // grid angular frequency [rad/s]

// ---- measured inputs (wired to the block inputs) ----------------------
double v_dc = xIN[0];              // DC bus voltage, from VOLT_DC probe
double i_l  = xIN[1];              // inductor current, from AMP_IL probe

// ---- 1) outer voltage loop: bus regulation -> current amplitude -------
// Integral-only and deliberately SLOW: it must not react to the
// unavoidable 100 Hz bus ripple. (The previous version of this example
// fed the ripple straight into the duty cycle through a proportional
// term, which drove the bus into a 31..89 V limit cycle.)
double e_v = v_ref - v_dc;         // bus voltage error [V]
i_amp = i_amp + 40.0 * e_v * dt;   // integral action [A]
if (i_amp < 0.0) {
    i_amp = 0.0;                   // anti-windup: a diode rectifier
}
if (i_amp > 12.0) {
    i_amp = 12.0;                  // cannot push power back upstream
}

// ---- 2) current reference: rectified sine, in phase with the grid -----
double shape = abs(sin(omega * t));       // |sin| template, phase-locked
                                          // to the 50 Hz grid
double i_ref = i_amp * shape;             // instantaneous current ref [A]

// ---- 3) inner current loop: duty feedforward + PI ----------------------
// Averaged boost stage: L*di/dt = |v_grid| - (1-duty)*v_dc.
// The steady-state duty  d_ff = 1 - |v_grid|/v_dc  is fed forward, so
// the PI only trims the remaining error and the loop gain stays
// roughly constant over the grid period.
double v_rec = v_pk * shape;              // rectified grid voltage [V]
double d_ff  = 1.0 - v_rec / max(v_dc, 1.0);
double e_i  = i_ref - i_l;                // current tracking error [A]
i_int = i_int + 3000.0 * e_i * dt;        // PI integral term
double duty = d_ff + 0.1 * e_i + i_int;   // PI output = total duty cycle
if (duty < 0.0) {
    duty = 0.0;                           // duty limits: 0 = diode-only
}
if (duty > 0.95) {
    duty = 0.95;                          // 0.95 leaves off-time remaining
}

// ---- 4) PWM generation at f_sw -----------------------------------------
double t_cycle = t % (1.0 / f_sw);        // position within switching period
if (t_cycle < duty / f_sw) {
    yOUT[0] = 1.0;    // switch ON : inductor charges, current rises
} else {
    yOUT[0] = 0.0;    // switch OFF: inductor + grid feed C_dc and load
}
return yOUT;
<\\sourceCode>
<staticCode>
<\\staticCode>
<importCode>
<\\importCode>
<staticVariables>
double i_amp = 6.0;   // outer-loop state: grid current amplitude [A],
                      // starts near the operating point sqrt(2)*P/Vrms = 5.9 A
double i_int = 0.0;   // inner-loop state: PI integrator
<\\staticVariables>
<\\ElementCONTROL>

c (3)
<ElementCONTROL>
labelAnfangsKnoten[] /pwm
labelEndKnoten[]
enabledShorted 1
typ 6
uniqueObjectIdentifier 2003
x 28
y 20
parameter[] 0.0
parameterString[] /S.1/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog GATE.1
coupledReferenceID[] 1007
copyCoupledReferenceID[] 1007
<\\ElementCONTROL>

verbindungControlANZAHL 3
verbindungCONTROL (0)
<Verbindung>
label v_dc_meas
x[] 12 13 14 15 16 17 18
y[] 20 20 20 20 20 20 20
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (1)
<Verbindung>
label i_l
x[] 12 13 14 14 14 14 15 16 17 18
y[] 24 24 24 23 22 21 21 21 21 21
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (2)
<Verbindung>
label pwm
x[] 22 23 24 25 26
y[] 20 20 20 20 20
enabled true
connectorType 1
<\\Verbindung>

sp (0)
<ElementSPECIAL>
labelAnfangsKnoten[] 
labelEndKnoten[] 
enabledShorted 1
typ 2001
uniqueObjectIdentifier 9001
x 46
y 4
parameter[] 
orientierung 501
idStringDialog NOTE.1
boxWidth 38
boxHeight 30
<markdownText>
# Active PFC Boost Pre-Regulator (50 Hz)

Power Factor Correction circuit shaping AC line current into a clean sinusoid in-phase with grid voltage.

### Architecture & Control Loops
- **Diode Bridge & Boost Stage**: Full-wave bridge rectifier feeding an $800\\text{ µH}$ boost inductor switched at 50 kHz.
- **Two-Loop Cascaded Controller**:
  1. **Outer Voltage Loop**: Slow PI controller maintaining $50\\text{ V DC}$ output across bulk capacitor.
  2. **Inner Current Loop**: Fast controller forcing inductor current to track rectified grid voltage shape $|\\sin(\\omega t)|$.
- **Result**: Unity power factor ($	ext{PF} > 0.99$) with minimal harmonic distortion (THD).

### Waveforms in Oscilloscope (Osci 1)
- **CH1 (v_dc_meas, V(ac1|ac2))**: Regulated 50 V DC bus voltage and 50 Hz AC grid voltage.
- **CH2 (i_l, pwm)**: High-frequency boost inductor current tracking sinusoidal envelope and PWM gate drive.
<\\markdownText>
<\\ElementSPECIAL>

tDURATION 0.060
dt 1.0E-6
solverType 0
FileVersion 1
dataContainerSignals[] /v_dc_meas/V(ac1|ac2)/i_l/pwm
<graphRegisters>
count 1
<graphRegister>
id osci_1
name Osci 1
numGraphs 2
signals v_dc_meas,V(ac1|ac2),i_l,pwm
signalGraphs v_dc_meas:1,V(ac1|ac2):1,i_l:2,pwm:2
hiddenSignals 
yScaleMode auto
cursorsEnabled false
<\\graphRegister>
<\\graphRegisters>
`;

export const SPWM_INVERTER_DEADTIME_IPES = `
verbindungLK (0)
<Verbindung>
label dc_pos
x[] 6 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 
y[] 9 7 7 7 7 7 7 7 7 7 7 7 7 7 7 7 7 7 7 7 7 7 
enabledShorted 0
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungLK (1)
<Verbindung>
label 0
x[] 6 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 
y[] 13 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 
enabledShorted 0
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungLK (2)
<Verbindung>
label out_a
x[] 16 15 14 13 12 11 10 10 10 10 10 10 10 10 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 
y[] 11 11 11 11 11 11 11 10 9 8 7 6 5 4 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 
enabledShorted 0
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungLK (3)
<Verbindung>
label out_b
x[] 26 25 24 23 22 21 20 20 20 20 20 20 20 20 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 44 44 44 44 44 44 
y[] 11 11 11 11 11 11 11 12 13 14 15 16 17 18 19 19 19 19 19 19 19 19 19 19 19 19 19 19 19 19 19 19 19 19 19 19 19 19 19 18 17 16 15 14 13 
enabledShorted 0
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungLK (4)
<Verbindung>
label out_b
x[] 38 38 38 38 38 38 38 
y[] 19 18 17 16 15 14 13 
enabledShorted 0
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungLK (5)
<Verbindung>
label load_p
x[] 36 37 38 39 40 41 42 43 44 44 44 44 44 44 44 
y[] 3 3 3 3 3 3 3 3 3 4 5 6 7 8 9 
enabledShorted 0
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungLK (6)
<Verbindung>
label load_p
x[] 38 38 38 38 38 38 38 
y[] 3 4 5 6 7 8 9 
enabledShorted 0
parentSheetIdentifier 0
connectorType 0
<\\Verbindung>

verbindungCONTROL (7)
<Verbindung>
label v_ref
x[] 8 9 10 
y[] 20 20 20 
enabledShorted 0
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungCONTROL (8)
<Verbindung>
label v_meas
x[] 8 9 10 10 
y[] 24 24 24 21 
enabledShorted 0
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungCONTROL (9)
<Verbindung>
label v_err
x[] 14 15 16 
y[] 20 20 20 
enabledShorted 0
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungCONTROL (10)
<Verbindung>
label v_ctrl
x[] 20 21 22 
y[] 20 20 20 
enabledShorted 0
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungCONTROL (11)
<Verbindung>
label v_mod
x[] 26 27 28 
y[] 20 20 20 
enabledShorted 0
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungCONTROL (12)
<Verbindung>
label v_tri
x[] 26 27 28 28 
y[] 26 26 26 21 
enabledShorted 0
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungCONTROL (13)
<Verbindung>
label pwm
x[] 32 33 34 
y[] 20 20 20 
enabledShorted 0
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungCONTROL (14)
<Verbindung>
label gate_hi
x[] 38 39 40 41 42 43 44 45 46 
y[] 20 20 20 20 20 20 20 20 20 
enabledShorted 0
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungCONTROL (15)
<Verbindung>
label gate_hi
x[] 44 44 44 44 44 44 44 45 46 
y[] 20 21 22 23 24 25 26 26 26 
enabledShorted 0
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungCONTROL (16)
<Verbindung>
label gate_lo
x[] 38 39 39 39 39 39 39 39 39 39 39 39 39 40 41 42 43 44 45 46 
y[] 21 21 22 23 24 25 26 27 28 29 30 31 32 32 32 32 32 32 32 32 
enabledShorted 0
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

verbindungCONTROL (17)
<Verbindung>
label gate_lo
x[] 44 44 44 44 44 44 44 45 46 
y[] 32 33 34 35 36 37 38 38 38 
enabledShorted 0
parentSheetIdentifier 0
connectorType 1
<\\Verbindung>

e (18)
<ElementLK>
labelAnfangsKnoten[] /dc_pos
labelEndKnoten[] /0
enabledShorted 1
parentSheetIdentifier 0
typ 4
uniqueObjectIdentifier 1001
x 6
y 11
parameter[] 401.0 40.0 50.0 0.0 0.0 0.5 0.0 40.0 0.0 -40.0 0.0 1.0 1.7976931348623157E308 0.0 0.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog V_dc

<\\ElementLK>

e (19)
<ElementLK>
labelAnfangsKnoten[] /dc_pos
labelEndKnoten[] /out_a
enabledShorted 1
parentSheetIdentifier 0
typ 7
uniqueObjectIdentifier 1002
coupledReferenceID[] 2002 
x 12
y 9
parameter[] 1.0E7 0.01 1.0E7 0.0 0.0 0.0 3.0E-5 1.5E-5 0.0 0.0 0.0 0.0 1.0 
parameterString[] /GATE.1/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog S.1
<Verluste>
verlustTyp 1
rON 0.01
uf 0.0
kON 3.0E-5
kOFF 1.5E-5
uSWnorm 400.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>

<\\ElementLK>

e (20)
<ElementLK>
labelAnfangsKnoten[] /out_a
labelEndKnoten[] /0
enabledShorted 1
parentSheetIdentifier 0
typ 7
uniqueObjectIdentifier 1003
coupledReferenceID[] 2004 
x 12
y 13
parameter[] 1.0E7 0.01 1.0E7 0.0 0.0 0.0 3.0E-5 1.5E-5 0.0 0.0 0.0 0.0 1.0 
parameterString[] /GATE.2/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog S.2
<Verluste>
verlustTyp 1
rON 0.01
uf 0.0
kON 3.0E-5
kOFF 1.5E-5
uSWnorm 400.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>

<\\ElementLK>

e (21)
<ElementLK>
labelAnfangsKnoten[] /dc_pos
labelEndKnoten[] /out_b
enabledShorted 1
parentSheetIdentifier 0
typ 7
uniqueObjectIdentifier 1004
coupledReferenceID[] 2009 
x 22
y 9
parameter[] 1.0E7 0.01 1.0E7 0.0 0.0 0.0 3.0E-5 1.5E-5 0.0 0.0 0.0 0.0 1.0 
parameterString[] /GATE.3/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog S.3
<Verluste>
verlustTyp 1
rON 0.01
uf 0.0
kON 3.0E-5
kOFF 1.5E-5
uSWnorm 400.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>

<\\ElementLK>

e (22)
<ElementLK>
labelAnfangsKnoten[] /out_b
labelEndKnoten[] /0
enabledShorted 1
parentSheetIdentifier 0
typ 7
uniqueObjectIdentifier 1005
coupledReferenceID[] 2008 
x 22
y 13
parameter[] 1.0E7 0.01 1.0E7 0.0 0.0 0.0 3.0E-5 1.5E-5 0.0 0.0 0.0 0.0 1.0 
parameterString[] /GATE.4/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog S.4
<Verluste>
verlustTyp 1
rON 0.01
uf 0.0
kON 3.0E-5
kOFF 1.5E-5
uSWnorm 400.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>

<\\ElementLK>

e (23)
<ElementLK>
labelAnfangsKnoten[] /out_a
labelEndKnoten[] /dc_pos
enabledShorted 1
parentSheetIdentifier 0
typ 6
uniqueObjectIdentifier 1006
x 16
y 9
parameter[] 1.0E7 0.7 0.001 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 501
idStringDialog D.1
<Verluste>
verlustTyp 1
rON 0.001
uf 0.7
kON 0.0
kOFF 0.0
uSWnorm 400.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>

<\\ElementLK>

e (24)
<ElementLK>
labelAnfangsKnoten[] /0
labelEndKnoten[] /out_a
enabledShorted 1
parentSheetIdentifier 0
typ 6
uniqueObjectIdentifier 1007
x 16
y 13
parameter[] 1.0E7 0.7 0.001 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 501
idStringDialog D.2
<Verluste>
verlustTyp 1
rON 0.001
uf 0.7
kON 0.0
kOFF 0.0
uSWnorm 400.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>

<\\ElementLK>

e (25)
<ElementLK>
labelAnfangsKnoten[] /out_b
labelEndKnoten[] /dc_pos
enabledShorted 1
parentSheetIdentifier 0
typ 6
uniqueObjectIdentifier 1008
x 26
y 9
parameter[] 1.0E7 0.7 0.001 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 501
idStringDialog D.3
<Verluste>
verlustTyp 1
rON 0.001
uf 0.7
kON 0.0
kOFF 0.0
uSWnorm 400.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>

<\\ElementLK>

e (26)
<ElementLK>
labelAnfangsKnoten[] /0
labelEndKnoten[] /out_b
enabledShorted 1
parentSheetIdentifier 0
typ 6
uniqueObjectIdentifier 1009
x 26
y 13
parameter[] 1.0E7 0.7 0.001 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 501
idStringDialog D.4
<Verluste>
verlustTyp 1
rON 0.001
uf 0.7
kON 0.0
kOFF 0.0
uSWnorm 400.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>

<\\ElementLK>

e (27)
<ElementLK>
labelAnfangsKnoten[] /out_a
labelEndKnoten[] /load_p
enabledShorted 1
parentSheetIdentifier 0
typ 2
uniqueObjectIdentifier 1010
x 34
y 3
parameter[] 0.004 0.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
idStringDialog L_filter

<\\ElementLK>

e (28)
<ElementLK>
labelAnfangsKnoten[] /load_p
labelEndKnoten[] /out_b
enabledShorted 1
parentSheetIdentifier 0
typ 3
uniqueObjectIdentifier 1011
x 38
y 11
parameter[] 2.0E-5 0.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog C_filter

<\\ElementLK>

e (29)
<ElementLK>
labelAnfangsKnoten[] /load_p
labelEndKnoten[] /out_b
enabledShorted 1
parentSheetIdentifier 0
typ 1
uniqueObjectIdentifier 1012
x 44
y 11
parameter[] 10.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog R_load

<\\ElementLK>

c (30)
<ElementCONTROL>
labelAnfangsKnoten[]
labelEndKnoten[] /v_ref
enabledShorted 1
parentSheetIdentifier 0
typ 1004
uniqueObjectIdentifier 2001
x 6
y 20
parameter[] 402.0 20.0 50.0 0.0 0.0 0.5 0.0 0.0 
parameterString[]
nameOpt[]
orientierung 503
idStringDialog V_REF
<detail>
typQuelle 402
anteilDC 0.0
amplitudeAC 20.0
frequenz 50.0
tastverhaeltnis 0.5
phase 0.0
<\\detail>

<\\ElementCONTROL>

c (31)
<ElementCONTROL>
labelAnfangsKnoten[]
labelEndKnoten[] /v_meas
enabledShorted 1
parentSheetIdentifier 0
typ 1001
uniqueObjectIdentifier 2005
x 6
y 24
parameter[] 0.0 
parameterString[] /load_p/out_b/0
nameOpt[]
orientierung 503
idStringDialog VOLT_AC

<\\ElementCONTROL>

c (32)
<ElementCONTROL>
labelAnfangsKnoten[] /v_ref/v_meas
labelEndKnoten[] /v_err
enabledShorted 1
parentSheetIdentifier 0
typ 1017
uniqueObjectIdentifier 2011
x 12
y 20
parameter[] 0.0 
parameterString[]
nameOpt[]
orientierung 503
idStringDialog CTRL_SUB

<\\ElementCONTROL>

c (33)
<ElementCONTROL>
labelAnfangsKnoten[] /v_err
labelEndKnoten[] /v_ctrl
enabledShorted 1
parentSheetIdentifier 0
typ 1007
uniqueObjectIdentifier 2012
x 18
y 20
parameter[] 0.04 0.005 
parameterString[]
nameOpt[]
orientierung 503
idStringDialog CTRL_PI

<\\ElementCONTROL>

c (34)
<ElementCONTROL>
labelAnfangsKnoten[] /v_ctrl
labelEndKnoten[] /v_mod
enabledShorted 1
parentSheetIdentifier 0
typ 1021
uniqueObjectIdentifier 2013
x 24
y 20
parameter[] -0.95 0.95 
parameterString[]
nameOpt[]
orientierung 503
idStringDialog CTRL_LIM

<\\ElementCONTROL>

c (35)
<ElementCONTROL>
labelAnfangsKnoten[]
labelEndKnoten[] /v_tri
enabledShorted 1
parentSheetIdentifier 0
typ 1004
uniqueObjectIdentifier 2014
x 24
y 26
parameter[] 403.0 1.0 5000.0 0.0 0.0 0.5 0.0 0.0 
parameterString[]
nameOpt[]
orientierung 503
idStringDialog V_CARRIER
<detail>
typQuelle 403
anteilDC 0.0
amplitudeAC 1.0
frequenz 5000.0
tastverhaeltnis 0.5
phase 0.0
<\\detail>

<\\ElementCONTROL>

c (36)
<ElementCONTROL>
labelAnfangsKnoten[] /v_mod/v_tri
labelEndKnoten[] /pwm
enabledShorted 1
parentSheetIdentifier 0
typ 1010
uniqueObjectIdentifier 2015
x 30
y 20
parameter[] 0.0 
parameterString[]
nameOpt[]
orientierung 503
idStringDialog CTRL_CMP

<\\ElementCONTROL>

c (37)
<ElementCONTROL>
labelAnfangsKnoten[] /pwm
labelEndKnoten[] /gate_hi/gate_lo
enabledShorted 1
parentSheetIdentifier 0
typ 1037
uniqueObjectIdentifier 2016
x 36
y 20
parameter[] 1.0E-6 
parameterString[]
nameOpt[]
orientierung 503
idStringDialog CTRL_DT

<\\ElementCONTROL>

c (38)
<ElementCONTROL>
labelAnfangsKnoten[] /gate_hi
labelEndKnoten[]
enabledShorted 1
parentSheetIdentifier 0
typ 6
uniqueObjectIdentifier 2002
coupledReferenceID[] 1002 
x 48
y 20
parameter[] 0.0 
parameterString[] /S.1/NIX_NIX_NIX/0
nameOpt[]
orientierung 503
idStringDialog GATE.1

<\\ElementCONTROL>

c (39)
<ElementCONTROL>
labelAnfangsKnoten[] /gate_hi
labelEndKnoten[]
enabledShorted 1
parentSheetIdentifier 0
typ 6
uniqueObjectIdentifier 2008
coupledReferenceID[] 1005 
x 48
y 26
parameter[] 0.0 
parameterString[] /S.4/NIX_NIX_NIX/0
nameOpt[]
orientierung 503
idStringDialog GATE.4

<\\ElementCONTROL>

c (40)
<ElementCONTROL>
labelAnfangsKnoten[] /gate_lo
labelEndKnoten[]
enabledShorted 1
parentSheetIdentifier 0
typ 6
uniqueObjectIdentifier 2004
coupledReferenceID[] 1003 
x 48
y 32
parameter[] 0.0 
parameterString[] /S.2/NIX_NIX_NIX/0
nameOpt[]
orientierung 503
idStringDialog GATE.2

<\\ElementCONTROL>

c (41)
<ElementCONTROL>
labelAnfangsKnoten[] /gate_lo
labelEndKnoten[]
enabledShorted 1
parentSheetIdentifier 0
typ 6
uniqueObjectIdentifier 2009
coupledReferenceID[] 1004 
x 48
y 38
parameter[] 0.0 
parameterString[] /S.3/NIX_NIX_NIX/0
nameOpt[]
orientierung 503
idStringDialog GATE.3

<\\ElementCONTROL>

sp (42)
<ElementSPECIAL>
labelAnfangsKnoten[]
labelEndKnoten[]
enabledShorted 1
parentSheetIdentifier 0
typ 2001
uniqueObjectIdentifier 9001
x 6
y 37
parameter[] NaN 
parameterString[]
nameOpt[]
orientierung 501
idStringDialog NOTE.1
boxWidth 38.0
boxHeight 30.0
<markdownText>
# Closed-Loop SPWM Inverter with Dead-Time (50 Hz)

Sinusoidal PWM (SPWM) full-bridge inverter converting **40 V DC** into **50 Hz AC** with closed-loop voltage regulation and hardware dead-time protection.

### Control Pipeline Architecture
1. **Reference & Feedback**: \`V_REF\` generates a 50 Hz sinusoidal reference (20 V pk). \`VOLT_AC\` samples load voltage.
2. **Error & Regulation**: \`CTRL_SUB\` computes error $(v_{ref} - v_{meas})$, fed to \`CTRL_PI\` ($K_p=0.45, T_i=2\\text{ ms}$) and \`CTRL_LIM\` clamped to $[-1, 1]$.
3. **Modulation**: \`CTRL_CMP\` compares modulation signal with 10 kHz triangle carrier \`V_CARRIER\`.
4. **Shoot-Through Protection**: \`CTRL_DEADTIME\` inserts **1.0 µs dead-time** between complementary gate drives (\`gate_hi\` and \`gate_lo\`).

### Key Waveforms in Oscilloscope (Osci 1)
- **CH1 (v_ref, v_meas)**: 50 Hz reference wave and filtered AC output voltage.
- **CH2 (v_err)**: Dynamic regulation error.
- **CH3 (gate_hi / gate_lo)**: Non-overlapping gate drives preventing DC bus cross-conduction.
<\\markdownText>

<\\ElementSPECIAL>

optimizerName[]
optimizerValue[] null
<scripterCode>

<\\scripterCode>
<scripterImports>

<\\scripterImports>
<scripterDeclarations>
 
<\\scripterDeclarations>
<extraScriptSourceFiles>
 
<\\extraScriptSourceFiles>

DtStor 2026-11-07
tDURATION 0.04
dt 1.0E-6
tPAUSE -1.0
T_pre 0.0
dt_pre 1.0E-6
solverType 0
path 

dpix 16
fontSize 12
fontTyp Arial
fensterWidth -1
fensterHeight -1
worksheetSize 600x600
ANSICHT_SHOW_LK_NAME true
ANSICHT_SHOW_LK_PARAMETER true
ANSICHT_SHOW_LK_FLOWDIR false
ANSICHT_SHOW_LK_TEXTLINIE true
ANSICHT_SHOW_THERM_NAME true
ANSICHT_SHOW_THERM_PARAMETER true
ANSICHT_SHOW_THERM_FLOWDIR false
ANSICHT_SHOW_THERM_TEXTLINIE true
ANSICHT_SHOW_CONTROL_NAME true
ANSICHT_SHOW_CONTROL_PARAMETER true
ANSICHT_SHOW_CONTROL_TEXTLINIE true
FileVersion 1
UniqueFileId 0
dataContainerSignals[] /v_ref/v_meas/v_err/gate_hi/gate_lo
<graphRegisters>
count 1
<graphRegister>
id osci_1
name Osci 1
numGraphs 3
signals v_ref,v_meas,v_err,gate_hi,gate_lo
signalGraphs v_ref:1,v_meas:1,v_err:2,gate_hi:3,gate_lo:3,dc_pos:1
hiddenSignals 
yScaleMode auto
cursorsEnabled false
<\\graphRegister>
<\\graphRegisters>

=======================
`;


export const SAMPLED_BUCK_IPES = BUCK_CONVERTER_IPES;

export const EXAMPLES: CircuitExample[] = [
  {
    id: 'spwm-inverter-deadtime',
    name: 'Closed-Loop SPWM Inverter with Dead-Time (50 Hz)',
    category: 'Power Electronics',
    description: 'Full-bridge 50 Hz inverter with PI regulator and 1.0 µs dead-time shoot-through prevention.',
    content: SPWM_INVERTER_DEADTIME_IPES,
  },
  {
    id: 'buck',
    name: 'DC-DC Buck Converter (Sampled 0.5ms Control)',
    category: 'Power Electronics',
    description: '50V to 10V buck converter with 50 Hz input ripple, 2 kHz (0.5ms) discrete S/H control and feedforward.',
    content: BUCK_CONVERTER_IPES,
  },
  {
    id: 'sync-buck',
    name: 'Synchronous Buck Converter (50 kHz)',
    category: 'Power Electronics',
    description: 'High-efficiency step-down converter with complementary active MOSFET switching.',
    content: SYNC_BUCK_IPES,
  },
  {
    id: 'boost',
    name: 'DC-DC Boost Converter (Step-Up)',
    category: 'Power Electronics',
    description: 'Step-up converter (12V to 24V) with boost inductor and fast recovery diode.',
    content: BOOST_CONVERTER_IPES,
  },
  {
    id: 'buck-boost',
    name: 'Inverting Buck-Boost Converter (-16V)',
    category: 'Power Electronics',
    description: 'Negative voltage step-up/step-down converter (24V to -16V) with single inductor.',
    content: BUCK_BOOST_IPES,
  },
  {
    id: 'flyback',
    name: 'Flyback Isolated DC-DC Converter',
    category: 'Power Electronics',
    description: 'Galvanically isolated converter with magnetically coupled inductors (k=0.999).',
    content: FLYBACK_IPES,
  },
  {
    id: 'closed-loop-buck',
    name: 'Closed-Loop Regulated Buck (PI Control)',
    category: 'Power Electronics',
    description: 'Voltage-mode DC-DC converter regulated to 12.0V by embedded PI script block.',
    content: CLOSED_LOOP_BUCK_IPES,
  },
  {
    id: 'inverter',
    name: 'Single-Phase H-Bridge Inverter (50 Hz)',
    category: 'Power Electronics',
    description: 'Full-bridge DC-AC inverter converting 40V DC bus into 50 Hz sinusoidal AC.',
    content: INVERTER_IPES,
  },
  {
    id: 'pfc-boost',
    name: 'Active PFC Boost Pre-Regulator (50 Hz)',
    category: 'Power Electronics',
    description: 'Active Power Factor Correction converter regulating 24V AC to 50V DC bus.',
    content: PFC_BOOST_IPES,
  },
  {
    id: 'rectifier',
    name: 'Diode Bridge Rectifier (AC to DC)',
    category: 'Power Electronics',
    description: 'Full-bridge Graetz diode rectifier with capacitive reservoir smoothing filter.',
    content: RECTIFIER_CIRCUIT_IPES,
  },
  {
    id: 'three-scopes',
    name: 'Multi-Instrument RLC (Osci 1 & Osci 2)',
    category: 'Analog & Filter',
    description: 'Second-order series RLC resonator with multi-instrument oscilloscope monitoring.',
    content: THREE_SCOPES_RLC_IPES,
  },
  {
    id: 'rlc',
    name: 'RLC Resonant Circuit',
    category: 'Analog & Filter',
    description: 'Underdamped second-order series RLC transient step response.',
    content: RLC_CIRCUIT_IPES,
  },
  {
    id: 'rc',
    name: 'RC Low-Pass Filter',
    category: 'Analog & Filter',
    description: 'First-order RC filter demonstrating step response and exponential charging.',
    content: RC_FILTER_IPES,
  },
];
