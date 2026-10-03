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
verbindungLeistungskreisANZAHL 7
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
label 0
x[] 30 30 30 30 30 
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


export const EXAMPLES: CircuitExample[] = [
  {
    id: 'buck',
    name: 'DC-DC Buck Converter (50 kHz PWM)',
    category: 'Power Electronics',
    description: 'Step-down converter (24V to 12V) with controlled switch, freewheeling diode, LC filter, load resistor, and oscilloscope tracking V_out and I_L.',
    content: BUCK_CONVERTER_IPES,
  },
  {
    id: 'boost',
    name: 'DC-DC Boost Converter (Step-Up)',
    category: 'Power Electronics',
    description: 'Step-up converter (12V to 24V) with boost inductor, switch, diode, output filter capacitor, and oscilloscope tracking V_in, V_out, and I_L.',
    content: BOOST_CONVERTER_IPES,
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
