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
label out
x[] 26 27 28 29 30 31 32 33 34 35 36 37 38 
y[] 6 6 6 6 6 6 6 6 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (3)
<Verbindung>
label 0
x[] 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 
y[] 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 
enabled true
connectorType 0
<\\Verbindung>

verbindungControlANZAHL 3
verbindungCONTROL (0)
<Verbindung>
label pwm
x[] 10 11 12 13 14 
y[] 16 16 16 16 16 
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (1)
<Verbindung>
label v_out
x[] 28 29 30 31 32 32 
y[] 16 16 16 16 16 17 
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (2)
<Verbindung>
label i_L
x[] 28 29 30 31 32 32 
y[] 20 20 20 20 20 19 
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
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
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
labelAnfangsKnoten[] /0
labelEndKnoten[] /sw_node
enabledShorted 1
typ 6
uniqueObjectIdentifier 1003
x 18
y 8
parameter[] 0.01 0.0 0.01 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 501
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

e (3)
<ElementLK>
labelAnfangsKnoten[] /sw_node
labelEndKnoten[] /out
enabledShorted 1
typ 2
uniqueObjectIdentifier 1004
x 24
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
x 32
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
x 38
y 8
parameter[] 5.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog R_load
<\\ElementLK>

controlANZAHL 5

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
parameterString[] /S.1/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog GATE.1
coupledReferenceID[] 1002
copyCoupledReferenceID[] 1002
<\\ElementCONTROL>

c (2)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_out
enabledShorted 1
typ 1
uniqueObjectIdentifier 2003
x 26
y 16
parameter[] 0.0
parameterString[] /out/0/0
nameOpt[] null
orientierung 503
idStringDialog VOLT_OUT
<\\ElementCONTROL>

c (3)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /i_L
enabledShorted 1
typ 2
uniqueObjectIdentifier 2004
x 26
y 20
parameter[] 0.0
parameterString[] /L.1/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog AMP_IL
coupledReferenceID[] 1004
copyCoupledReferenceID[] 1004
<\\ElementCONTROL>

c (4)
<ElementCONTROL>
labelAnfangsKnoten[] /v_out/i_L
labelEndKnoten[] 
enabledShorted 1
typ 5
uniqueObjectIdentifier 2005
x 34
y 18
parameter[] 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog SCOPE.1
<detail>
tn 2
isShowName false
savedSignalNames[] /v_out/i_L
<\\detail>
<\\ElementCONTROL>

tDURATION 0.001
dt 2.0E-7
solverType 0
FileVersion 1
dataContainerSignals[] /v_out/i_L
`;

export const BOOST_CONVERTER_IPES = `
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
label out
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

verbindungControlANZAHL 4
verbindungCONTROL (0)
<Verbindung>
label pwm
x[] 10 11 12 13 14 
y[] 16 16 16 16 16 
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (1)
<Verbindung>
label v_in
x[] 28 29 30 31 32 33 34 34 
y[] 16 16 16 16 16 16 16 18 
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (2)
<Verbindung>
label v_out
x[] 28 29 30 31 32 33 34 
y[] 20 20 20 20 20 20 20 
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (3)
<Verbindung>
label i_L
x[] 28 29 30 31 32 33 34 34 
y[] 24 24 24 24 24 24 24 22 
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

controlANZAHL 6

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

c (2)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_in
enabledShorted 1
typ 1
uniqueObjectIdentifier 2003
x 26
y 16
parameter[] 0.0
parameterString[] /in/0/0
nameOpt[] null
orientierung 503
idStringDialog VOLT_IN
<\\ElementCONTROL>

c (3)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_out
enabledShorted 1
typ 1
uniqueObjectIdentifier 2004
x 26
y 20
parameter[] 0.0
parameterString[] /out/0/0
nameOpt[] null
orientierung 503
idStringDialog VOLT_OUT
<\\ElementCONTROL>

c (4)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /i_L
enabledShorted 1
typ 2
uniqueObjectIdentifier 2005
x 26
y 24
parameter[] 0.0
parameterString[] /L_boost/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog AMP_IL
coupledReferenceID[] 1002
copyCoupledReferenceID[] 1002
<\\ElementCONTROL>

c (5)
<ElementCONTROL>
labelAnfangsKnoten[] /v_in/v_out/i_L
labelEndKnoten[] 
enabledShorted 1
typ 5
uniqueObjectIdentifier 2006
x 36
y 20
parameter[] 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog SCOPE.1
<detail>
tn 3
isShowName false
savedSignalNames[] /v_in/v_out/i_L
<\\detail>
<\\ElementCONTROL>

tDURATION 0.002
dt 2.0E-7
solverType 0
FileVersion 1
dataContainerSignals[] /v_in/v_out/i_L
`;

export const RECTIFIER_CIRCUIT_IPES = `
verbindungLeistungskreisANZAHL 9
verbindungLK (0)
<Verbindung>
label ac1
x[] 6 7 8 9 10 11 12 13 14 
y[] 10 10 10 10 10 10 10 10 10 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (1)
<Verbindung>
label ac2
x[] 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 
y[] 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 14 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (2)
<Verbindung>
label ac2
x[] 22 22 22 22 22 
y[] 14 13 12 11 10 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (3)
<Verbindung>
label dc_plus
x[] 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 
y[] 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (4)
<Verbindung>
label 0
x[] 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 
y[] 18 18 18 18 18 18 18 18 18 18 18 18 18 18 18 18 18 18 18 18 18 18 18 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (5)
<Verbindung>
label dc_plus
x[] 30 30 30 30 30
y[] 6 7 8 9 10
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (6)
<Verbindung>
label dc_plus
x[] 36 36 36 36 36
y[] 6 7 8 9 10
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (7)
<Verbindung>
label 0
x[] 30 30 30 30 30
y[] 14 15 16 17 18
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (8)
<Verbindung>
label 0
x[] 36 36 36 36 36
y[] 14 15 16 17 18
enabled true
connectorType 0
<\\Verbindung>

verbindungControlANZAHL 3
verbindungCONTROL (0)
<Verbindung>
label v_ac
x[] 16 17 18 19 20 21 22 22 
y[] 24 24 24 24 24 24 24 26 
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (1)
<Verbindung>
label v_dc
x[] 16 17 18 19 20 21 22 
y[] 28 28 28 28 28 28 28 
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (2)
<Verbindung>
label i_load
x[] 16 17 18 19 20 21 22 22 
y[] 32 32 32 32 32 32 32 30 
enabled true
connectorType 1
<\\Verbindung>

elementANZAHL 7

e (0)
<ElementLK>
labelAnfangsKnoten[] /ac1
labelEndKnoten[] /ac2
enabledShorted 1
typ 4
uniqueObjectIdentifier 1001
x 6
y 12
parameter[] 401.0 34.0 50.0 0.0 0.0 0.5 0.0 34.0 0.0 -34.0 0.0 1.0 1.7976931348623157E308 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog V_ac
<\\ElementLK>

e (1)
<ElementLK>
labelAnfangsKnoten[] /ac1
labelEndKnoten[] /dc_plus
enabledShorted 1
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

e (2)
<ElementLK>
labelAnfangsKnoten[] /0
labelEndKnoten[] /ac1
enabledShorted 1
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

e (3)
<ElementLK>
labelAnfangsKnoten[] /ac2
labelEndKnoten[] /dc_plus
enabledShorted 1
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

e (4)
<ElementLK>
labelAnfangsKnoten[] /0
labelEndKnoten[] /ac2
enabledShorted 1
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

e (5)
<ElementLK>
labelAnfangsKnoten[] /dc_plus
labelEndKnoten[] /0
enabledShorted 1
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

e (6)
<ElementLK>
labelAnfangsKnoten[] /dc_plus
labelEndKnoten[] /0
enabledShorted 1
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

controlANZAHL 4

c (0)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_ac
enabledShorted 1
typ 1
uniqueObjectIdentifier 2001
x 14
y 24
parameter[] 0.0
parameterString[] /ac1/ac2/0
nameOpt[] null
orientierung 503
idStringDialog VOLT_AC
<\\ElementCONTROL>

c (1)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_dc
enabledShorted 1
typ 1
uniqueObjectIdentifier 2002
x 14
y 28
parameter[] 0.0
parameterString[] /dc_plus/0/0
nameOpt[] null
orientierung 503
idStringDialog VOLT_DC
<\\ElementCONTROL>

c (2)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /i_load
enabledShorted 1
typ 2
uniqueObjectIdentifier 2003
x 14
y 32
parameter[] 0.0
parameterString[] /R_load/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog AMP_LOAD
coupledReferenceID[] 1007
copyCoupledReferenceID[] 1007
<\\ElementCONTROL>

c (3)
<ElementCONTROL>
labelAnfangsKnoten[] /v_ac/v_dc/i_load
labelEndKnoten[] 
enabledShorted 1
typ 5
uniqueObjectIdentifier 2004
x 24
y 28
parameter[] 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog SCOPE.1
<detail>
tn 3
isShowName false
savedSignalNames[] /v_ac/v_dc/i_load
<\\detail>
<\\ElementCONTROL>

tDURATION 0.04
dt 2.0E-6
solverType 0
FileVersion 1
dataContainerSignals[] /v_ac/v_dc/i_load
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

verbindungControlANZAHL 5
verbindungCONTROL (0)
<Verbindung>
label v_in
x[] 16 17 18 19 20 21 22 22 
y[] 16 16 16 16 16 16 16 17 
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (1)
<Verbindung>
label v_out
x[] 16 17 18 19 20 21 22 22 
y[] 20 20 20 20 20 20 20 19 
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (2)
<Verbindung>
label v_R1
x[] 16 17 18 19 20 21 22 23 24 24 
y[] 26 26 26 26 26 26 26 26 26 28 
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (3)
<Verbindung>
label v_L1
x[] 16 17 18 19 20 21 22 23 24 
y[] 30 30 30 30 30 30 30 30 30 
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (4)
<Verbindung>
label v_C1
x[] 16 17 18 19 20 21 22 23 24 24 
y[] 34 34 34 34 34 34 34 34 34 32 
enabled true
connectorType 1
<\\Verbindung>

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

controlANZAHL 7

c (0)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_in
enabledShorted 1
typ 1
uniqueObjectIdentifier 2001
x 14
y 16
parameter[] 0.0
parameterString[] /V_in/0/0
nameOpt[] 
orientierung 503
idStringDialog VOLT_IN
<\\ElementCONTROL>

c (1)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_out
enabledShorted 1
typ 1
uniqueObjectIdentifier 2002
x 14
y 20
parameter[] 0.0
parameterString[] /V_out/0/0
nameOpt[] 
orientierung 503
idStringDialog VOLT_OUT
<\\ElementCONTROL>

c (2)
<ElementCONTROL>
labelAnfangsKnoten[] /v_in/v_out
labelEndKnoten[] 
enabledShorted 1
typ 5
uniqueObjectIdentifier 2003
x 24
y 18
parameter[] 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] 
orientierung 503
idStringDialog SCOPE.1
<\\ElementCONTROL>

c (3)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_R1
enabledShorted 1
typ 1
uniqueObjectIdentifier 2004
x 14
y 26
parameter[] 0.0
parameterString[] /V_in/n_mid/0
nameOpt[] 
orientierung 503
idStringDialog VOLT_R1
<\\ElementCONTROL>

c (4)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_L1
enabledShorted 1
typ 1
uniqueObjectIdentifier 2005
x 14
y 30
parameter[] 0.0
parameterString[] /n_mid/V_out/0
nameOpt[] 
orientierung 503
idStringDialog VOLT_L1
<\\ElementCONTROL>

c (5)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_C1
enabledShorted 1
typ 1
uniqueObjectIdentifier 2006
x 14
y 34
parameter[] 0.0
parameterString[] /V_out/0/0
nameOpt[] 
orientierung 503
idStringDialog VOLT_C1
<\\ElementCONTROL>

c (6)
<ElementCONTROL>
labelAnfangsKnoten[] /v_R1/v_L1/v_C1
labelEndKnoten[] 
enabledShorted 1
typ 5
uniqueObjectIdentifier 2007
x 26
y 30
parameter[] 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] 
orientierung 503
idStringDialog SCOPE.2
<\\ElementCONTROL>

tDURATION 0.01
dt 1e-06
solverType 0
FileVersion 1
dataContainerSignals[] /v_in/v_out/v_R1/v_L1/v_C1
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

verbindungControlANZAHL 3
verbindungCONTROL (0)
<Verbindung>
label v_in
x[] 16 17 18 19 20 21 22 22 
y[] 16 16 16 16 16 16 16 18 
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (1)
<Verbindung>
label v_out
x[] 16 17 18 19 20 21 22 
y[] 20 20 20 20 20 20 20 
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (2)
<Verbindung>
label i_L
x[] 16 17 18 19 20 21 22 22 
y[] 24 24 24 24 24 24 24 22 
enabled true
connectorType 1
<\\Verbindung>

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

controlANZAHL 4

c (0)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_in
enabledShorted 1
typ 1
uniqueObjectIdentifier 2001
x 14
y 16
parameter[] 0.0
parameterString[] /in/0/0
nameOpt[] null
orientierung 503
idStringDialog VOLT_IN
<\\ElementCONTROL>

c (1)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_out
enabledShorted 1
typ 1
uniqueObjectIdentifier 2002
x 14
y 20
parameter[] 0.0
parameterString[] /out/0/0
nameOpt[] null
orientierung 503
idStringDialog VOLT_OUT
<\\ElementCONTROL>

c (2)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /i_L
enabledShorted 1
typ 2
uniqueObjectIdentifier 2003
x 14
y 24
parameter[] 0.0
parameterString[] /L1/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog AMP_IL
coupledReferenceID[] 1003
copyCoupledReferenceID[] 1003
<\\ElementCONTROL>

c (3)
<ElementCONTROL>
labelAnfangsKnoten[] /v_in/v_out/i_L
labelEndKnoten[] 
enabledShorted 1
typ 5
uniqueObjectIdentifier 2004
x 24
y 20
parameter[] 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog SCOPE.1
<detail>
tn 3
isShowName false
savedSignalNames[] /v_in/v_out/i_L
<\\detail>
<\\ElementCONTROL>

tDURATION 0.005
dt 1e-06
solverType 0
FileVersion 1
dataContainerSignals[] /v_in/v_out/i_L
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

verbindungControlANZAHL 2
verbindungCONTROL (0)
<Verbindung>
label v_in
x[] 14 15 16 17 18 18 
y[] 16 16 16 16 16 17 
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (1)
<Verbindung>
label v_out
x[] 14 15 16 17 18 18 
y[] 20 20 20 20 20 19 
enabled true
connectorType 1
<\\Verbindung>

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

controlANZAHL 3

c (0)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_in
enabledShorted 1
typ 1
uniqueObjectIdentifier 2001
x 12
y 16
parameter[] 0.0
parameterString[] /in/0/0
nameOpt[] null
orientierung 503
idStringDialog VOLT_IN
<\\ElementCONTROL>

c (1)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_out
enabledShorted 1
typ 1
uniqueObjectIdentifier 2002
x 12
y 20
parameter[] 0.0
parameterString[] /out/0/0
nameOpt[] null
orientierung 503
idStringDialog VOLT_OUT
<\\ElementCONTROL>

c (2)
<ElementCONTROL>
labelAnfangsKnoten[] /v_in/v_out
labelEndKnoten[] 
enabledShorted 1
typ 5
uniqueObjectIdentifier 2003
x 20
y 18
parameter[] 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog SCOPE.1
<detail>
tn 2
isShowName false
savedSignalNames[] /v_in/v_out
<\\detail>
<\\ElementCONTROL>

tDURATION 0.01
dt 1e-06
solverType 0
FileVersion 1
dataContainerSignals[] /v_in/v_out
`;

export const RC_CLASSIC_IPES = `
verbindungLeistungskreisANZAHL 3
verbindungLK (0)
<Verbindung>
label n1
x[] 7 8 9 10 11 
y[] 9 9 9 9 9 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (1)
<Verbindung>
label u_out
x[] 15 16 17 18 19 
y[] 9 9 9 9 9 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (2)
<Verbindung>
label gnd
x[] 19 18 17 16 15 14 13 12 11 10 9 8 7 
y[] 13 13 13 13 13 13 13 13 13 13 13 13 13 
enabled true
connectorType 0
<\\Verbindung>

verbindungControlANZAHL 1
verbindungCONTROL (0)
<Verbindung>
label u_out
x[] 27 28 
y[] 10 10 
enabled true
connectorType 1
<\\Verbindung>

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
y 9
parameter[] 10.0 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
idStringDialog R.1
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
x 25
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
x 30
y 10
parameter[] 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] 
orientierung 503
idStringDialog SCOPE.1
shiftLabelsIn[] true
<\\ElementCONTROL>

tDURATION 5.0E-3
dt 1.0E-6
solverType 0
FileVersion 1
dataContainerSignals[] /u_out
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

verbindungControlANZAHL 5
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

verbindungCONTROL (2)
<Verbindung>
label v_out
x[] 26 27 28 29 30 31 32 33 34 34 34
y[] 18 18 18 18 18 18 18 18 18 19 20
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (3)
<Verbindung>
label v_sw
x[] 26 27 28 29 30 31 32 33 34
y[] 22 22 22 22 22 22 22 22 22
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (4)
<Verbindung>
label i_L
x[] 26 27 28 29 30 31 32 33 34 34 34
y[] 26 26 26 26 26 26 26 26 26 25 24
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

controlANZAHL 8

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

c (4)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_out
enabledShorted 1
typ 1
uniqueObjectIdentifier 2005
x 24
y 18
parameter[] 0.0
parameterString[] /out/0/0
nameOpt[] null
orientierung 503
idStringDialog VOLT_OUT
<\\ElementCONTROL>

c (5)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_sw
enabledShorted 1
typ 1
uniqueObjectIdentifier 2006
x 24
y 22
parameter[] 0.0
parameterString[] /sw_node/0/0
nameOpt[] null
orientierung 503
idStringDialog VOLT_SW
<\\ElementCONTROL>

c (6)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /i_L
enabledShorted 1
typ 2
uniqueObjectIdentifier 2007
x 24
y 26
parameter[] 0.0
parameterString[] /L.1/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog AMP_IL
coupledReferenceID[] 1004
copyCoupledReferenceID[] 1004
<\\ElementCONTROL>

c (7)
<ElementCONTROL>
labelAnfangsKnoten[] /v_out/v_sw/i_L
labelEndKnoten[] 
enabledShorted 1
typ 5
uniqueObjectIdentifier 2008
x 36
y 22
parameter[] 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog SCOPE.1
<detail>
tn 3
isShowName false
savedSignalNames[] /v_out/v_sw/i_L
<\\detail>
<\\ElementCONTROL>

tDURATION 0.001
dt 2.0E-7
solverType 0
FileVersion 1
dataContainerSignals[] /v_out/v_sw/i_L
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

verbindungControlANZAHL 4
verbindungCONTROL (0)
<Verbindung>
label pwm
x[] 10 11 12 13 14 
y[] 16 16 16 16 16 
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (1)
<Verbindung>
label v_in
x[] 28 29 30 31 32 33 34 34 
y[] 16 16 16 16 16 16 16 18 
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (2)
<Verbindung>
label v_out
x[] 28 29 30 31 32 33 34 
y[] 20 20 20 20 20 20 20 
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (3)
<Verbindung>
label i_L
x[] 28 29 30 31 32 33 34 34 
y[] 24 24 24 24 24 24 24 22 
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

controlANZAHL 6

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

c (2)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_in
enabledShorted 1
typ 1
uniqueObjectIdentifier 2003
x 26
y 16
parameter[] 0.0
parameterString[] /in/0/0
nameOpt[] null
orientierung 503
idStringDialog VOLT_IN
<\\ElementCONTROL>

c (3)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_out
enabledShorted 1
typ 1
uniqueObjectIdentifier 2004
x 26
y 20
parameter[] 0.0
parameterString[] /v_neg/0/0
nameOpt[] null
orientierung 503
idStringDialog VOLT_OUT
<\\ElementCONTROL>

c (4)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /i_L
enabledShorted 1
typ 2
uniqueObjectIdentifier 2005
x 26
y 24
parameter[] 0.0
parameterString[] /L.1/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog AMP_IL
coupledReferenceID[] 1003
copyCoupledReferenceID[] 1003
<\\ElementCONTROL>

c (5)
<ElementCONTROL>
labelAnfangsKnoten[] /v_in/v_out/i_L
labelEndKnoten[] 
enabledShorted 1
typ 5
uniqueObjectIdentifier 2006
x 36
y 20
parameter[] 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog SCOPE.1
<detail>
tn 3
isShowName false
savedSignalNames[] /v_in/v_out/i_L
<\\detail>
<\\ElementCONTROL>

tDURATION 0.005
dt 2.0E-7
solverType 0
FileVersion 1
dataContainerSignals[] /v_in/v_out/i_L
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

verbindungControlANZAHL 7
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

verbindungCONTROL (4)
<Verbindung>
label v_ac
x[] 36 37 38 39 40 40 40
y[] 23 23 23 23 23 24 25
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (5)
<Verbindung>
label v_ab
x[] 36 37 38 39 40
y[] 27 27 27 27 27
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (6)
<Verbindung>
label i_load
x[] 36 37 38 39 40 40 40
y[] 31 31 31 31 31 30 29
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

controlANZAHL 10

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

c (6)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_ac
enabledShorted 1
typ 1
uniqueObjectIdentifier 2005
x 34
y 23
parameter[] 0.0
parameterString[] /load_p/out_b/0
nameOpt[] null
orientierung 503
idStringDialog VOLT_AC
<\\ElementCONTROL>

c (7)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_ab
enabledShorted 1
typ 1
uniqueObjectIdentifier 2006
x 34
y 27
parameter[] 0.0
parameterString[] /out_a/out_b/0
nameOpt[] null
orientierung 503
idStringDialog VOLT_AB
<\\ElementCONTROL>

c (8)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /i_load
enabledShorted 1
typ 2
uniqueObjectIdentifier 2007
x 34
y 31
parameter[] 0.0
parameterString[] /L_filter/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog AMP_LOAD
coupledReferenceID[] 1010
copyCoupledReferenceID[] 1010
<\\ElementCONTROL>

c (9)
<ElementCONTROL>
labelAnfangsKnoten[] /v_ac/v_ab/i_load
labelEndKnoten[] 
enabledShorted 1
typ 5
uniqueObjectIdentifier 2010
x 42
y 27
parameter[] 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog SCOPE.1
<detail>
tn 3
isShowName false
savedSignalNames[] /v_ac/v_ab/i_load
<\\detail>
<\\ElementCONTROL>

tDURATION 0.04
dt 2.0E-6
solverType 0
FileVersion 1
dataContainerSignals[] /v_ac/v_ab/i_load
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

verbindungControlANZAHL 3
verbindungCONTROL (0)
<Verbindung>
label pwm
x[] 10 11 12 
y[] 16 16 16 
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (1)
<Verbindung>
label v_out
x[] 28 29 30 31 32 33 34 34 
y[] 16 16 16 16 16 16 16 17 
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (2)
<Verbindung>
label v_sw
x[] 28 29 30 31 32 33 34 34 
y[] 20 20 20 20 20 20 20 19 
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

controlANZAHL 5

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

c (2)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_out
enabledShorted 1
typ 1
uniqueObjectIdentifier 2003
x 26
y 16
parameter[] 0.0
parameterString[] /out/0/0
nameOpt[] null
orientierung 503
idStringDialog VOLT_OUT
<\\ElementCONTROL>

c (3)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_sw
enabledShorted 1
typ 1
uniqueObjectIdentifier 2004
x 26
y 20
parameter[] 0.0
parameterString[] /p_sw/0/0
nameOpt[] null
orientierung 503
idStringDialog VOLT_SW
<\\ElementCONTROL>

c (4)
<ElementCONTROL>
labelAnfangsKnoten[] /v_out/v_sw
labelEndKnoten[] 
enabledShorted 1
typ 5
uniqueObjectIdentifier 2005
x 36
y 18
parameter[] 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog SCOPE.1
<detail>
tn 2
isShowName false
savedSignalNames[] /v_out/v_sw
<\\detail>
<\\ElementCONTROL>

tDURATION 0.005
dt 2.0E-7
solverType 0
FileVersion 1
dataContainerSignals[] /v_out/v_sw
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

verbindungControlANZAHL 3
verbindungCONTROL (0)
<Verbindung>
label v_meas
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

verbindungCONTROL (2)
<Verbindung>
label v_out_scope
x[] 10 10 11 12 12 12 12 12 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32
y[] 16 17 17 17 18 19 20 21 22 22 22 22 22 22 22 22 22 22 22 22 22 22 22 22 22 22 22 22 22
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

controlANZAHL 4

c (0)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_meas
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
labelAnfangsKnoten[] /v_meas
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

c (3)
<ElementCONTROL>
labelAnfangsKnoten[] /v_out_scope
labelEndKnoten[] 
enabledShorted 1
typ 5
uniqueObjectIdentifier 2004
x 34
y 22
parameter[] 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog SCOPE.1
<detail>
tn 1
isShowName false
savedSignalNames[] /v_out_scope
<\\detail>
<\\ElementCONTROL>

tDURATION 0.003
dt 2.0E-7
solverType 0
FileVersion 1
dataContainerSignals[] /v_out_scope
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

controlANZAHL 6

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
labelAnfangsKnoten[]
labelEndKnoten[] /u_grid
enabledShorted 1
typ 1
uniqueObjectIdentifier 2006
x 10
y 28
parameter[] 0.0
parameterString[] /ac1/ac2/0
nameOpt[] null
orientierung 503
idStringDialog VOLT_GRID
<\\ElementCONTROL>

c (3)
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
// =====================================================================
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
// =====================================================================

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

c (4)
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

c (5)
<ElementCONTROL>
labelAnfangsKnoten[] /v_dc_meas/i_l/u_grid
labelEndKnoten[]
enabledShorted 1
typ 5
uniqueObjectIdentifier 2004
x 38
y 20
parameter[]
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog SCOPE.1
<detail>
tn 3
isShowName false
savedSignalNames[] /v_dc_meas/i_l/u_grid
<\\detail>
<\\ElementCONTROL>

verbindungControlANZAHL 6
verbindungCONTROL (0)
<Verbindung>
label v_dc_meas
x[] 12 13 14 15 16 17 18 18
y[] 20 20 20 20 20 20 20 19
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

verbindungCONTROL (3)
<Verbindung>
label v_dc_meas
x[] 13 13 13 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 36
y[] 20 19 18 17 17 17 17 17 17 17 17 17 17 17 17 17 17 17 17 17 17 17 17 17 17 17 17 18
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (4)
<Verbindung>
label i_l
x[] 13 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 35 35 35 35 35 36
y[] 24 25 25 25 25 25 25 25 25 25 25 25 25 25 25 25 25 25 25 25 25 25 25 25 24 23 22 21 20 20
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (5)
<Verbindung>
label u_grid
x[] 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 36 36 36 36 36 36
y[] 28 28 28 28 28 28 28 28 28 28 28 28 28 28 28 28 28 28 28 28 28 28 28 28 28 27 26 25 24 23 22
enabled true
connectorType 1
<\\Verbindung>

tDURATION 0.060
dt 1.0E-6
solverType 0
FileVersion 1
dataContainerSignals[] /v_dc_meas/i_l/u_grid
`;

export const EXAMPLES: CircuitExample[] = [
  {
    id: 'buck',
    name: 'DC-DC Buck Converter (50 kHz PWM)',
    category: 'Power Electronics',
    description: 'Step-down converter (24V to 12V) with controlled switch, freewheeling diode, LC filter, load resistor, and oscilloscope tracking V_out and I_L.',
    content: BUCK_CONVERTER_IPES,
  },
  {
    id: 'sync-buck',
    name: 'Synchronous Buck Converter (50 kHz)',
    category: 'Power Electronics',
    description: 'High-efficiency step-down converter with complementary high-side and low-side active switches, body-diode freewheeling, and output filter.',
    content: SYNC_BUCK_IPES,
  },
  {
    id: 'boost',
    name: 'DC-DC Boost Converter (Step-Up)',
    category: 'Power Electronics',
    description: 'Step-up converter (12V to 24V) with boost inductor, switch, diode, output filter capacitor, and oscilloscope tracking V_in, V_out, and I_L.',
    content: BOOST_CONVERTER_IPES,
  },
  {
    id: 'buck-boost',
    name: 'Inverting Buck-Boost Converter (-16V)',
    category: 'Power Electronics',
    description: 'Negative voltage step-up/step-down converter (24V to -16V at D=0.40) with single inductor energy storage and diode-swapped polarity.',
    content: BUCK_BOOST_IPES,
  },
  {
    id: 'flyback',
    name: 'Flyback Isolated DC-DC Converter',
    category: 'Power Electronics',
    description: 'Galvanically isolated converter with coupled inductors (k=0.999), primary high-side switch, secondary fast-recovery diode, and bulk filter.',
    content: FLYBACK_IPES,
  },
  {
    id: 'closed-loop-buck',
    name: 'Closed-Loop Regulated Buck (PI Control)',
    category: 'Power Electronics',
    description: 'Voltage-mode closed-loop DC-DC converter featuring an embedded microcontroller PI control script block maintaining 12.0V output under regulation.',
    content: CLOSED_LOOP_BUCK_IPES,
  },
  {
    id: 'inverter',
    name: 'Single-Phase H-Bridge Inverter (50 Hz)',
    category: 'Power Electronics',
    description: 'Full-bridge DC-AC inverter with complementary gate pulses converting 40V DC bus into 50 Hz sinusoidal AC across an LC filter and 10 ohm load.',
    content: INVERTER_IPES,
  },
  {
    id: 'pfc-boost',
    name: 'Active PFC Boost Pre-Regulator (50 Hz)',
    category: 'Power Electronics',
    description: 'Power Factor Correction circuit: diode bridge, 800 µH boost inductor, 50 kHz MOSFET, and a two-loop controller (slow bus-voltage loop + sinusoidal current loop) regulating 24 V AC to a 50 V DC bus at near-unity power factor. Scope shows bus voltage, inductor current, and grid voltage.',
    content: PFC_BOOST_IPES,
  },
  {
    id: 'rectifier',
    name: 'Diode Bridge Rectifier (AC to DC)',
    category: 'Power Electronics',
    description: 'Full-bridge diode rectifier (Graetz bridge) converting 50 Hz AC mains to smoothed DC bus voltage across a reservoir capacitor and resistive load.',
    content: RECTIFIER_CIRCUIT_IPES,
  },
  {
    id: 'three-scopes',
    name: 'Multi-Scope RLC (SCOPE.1 & SCOPE.2)',
    category: 'Analog & Filter',
    description: 'Multi-instrument demonstration: SCOPE.1 (V_in, V_out) and SCOPE.2 (V_R1, V_L1, V_C1) across a second-order series RLC resonator.',
    content: THREE_SCOPES_RLC_IPES,
  },
  {
    id: 'rlc',
    name: 'RLC Resonant Circuit',
    category: 'Analog & Filter',
    description: 'Step response of a second-order RLC series resonator demonstrating underdamped transient oscillation and exponential decay.',
    content: RLC_CIRCUIT_IPES,
  },
  {
    id: 'rc',
    name: 'RC Low-Pass Filter',
    category: 'Analog & Filter',
    description: 'First-order RC low-pass filter demonstrating step response and exponential capacitor charging curve with oscilloscope.',
    content: RC_FILTER_IPES,
  },
  {
    id: 'rc-classic',
    name: 'RC Low-Pass (Classic Reference)',
    category: 'Analog & Filter',
    description: 'Bit-identical classic reference circuit matching legacy GeckoCIRCUITS solver benchmarks.',
    content: RC_CLASSIC_IPES,
  },
];
