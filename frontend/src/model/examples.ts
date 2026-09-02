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
verbindungLeistungskreisANZAHL 4
verbindungLK (0)
<Verbindung>
label NIX_NIX_NIX
x[] 4 5 6 7 8 
y[] 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (1)
<Verbindung>
label NIX_NIX_NIX
x[] 12 13 14 
y[] 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (2)
<Verbindung>
label u_out
x[] 18 19 20 21 22 
y[] 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (3)
<Verbindung>
label 0
x[] 22 21 20 19 18 17 16 15 14 13 12 11 10 9 8 7 6 5 4 
y[] 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 
enabled true
connectorType 0
<\\Verbindung>

elementANZAHL 4

e (0)
<ElementLK>
labelAnfangsKnoten[] /NIX_NIX_NIX
labelEndKnoten[] /0
enabledShorted 1
typ 4
uniqueObjectIdentifier 1001
x 4
y 8
parameter[] 401.0 24.0 50.0 0.0 0.0 0.5 0.0 24.0 0.0 -24.0 0.0 1.0 1.7976931348623157E308 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog V_in
<\\ElementLK>

e (1)
<ElementLK>
labelAnfangsKnoten[] /NIX_NIX_NIX
labelEndKnoten[] /NIX_NIX_NIX
enabledShorted 1
typ 1
uniqueObjectIdentifier 1002
x 10
y 6
parameter[] 5.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
idStringDialog R1
<\\ElementLK>

e (2)
<ElementLK>
labelAnfangsKnoten[] /NIX_NIX_NIX
labelEndKnoten[] /u_out
enabledShorted 1
typ 2
uniqueObjectIdentifier 1003
x 16
y 6
parameter[] 0.001 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
idStringDialog L1
<\\ElementLK>

e (3)
<ElementLK>
labelAnfangsKnoten[] /u_out
labelEndKnoten[] /0
enabledShorted 1
typ 3
uniqueObjectIdentifier 1004
x 22
y 8
parameter[] 0.00001 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog C1
<\\ElementLK>

tDURATION 0.01
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
dataContainerSignals[] /u_out
`;

export const BUCK_CONVERTER_IPES = `
verbindungLeistungskreisANZAHL 7
verbindungLK (0)
<Verbindung>
label z1
x[] 27 28 29 30 31 32 33 34 34 34 
y[] 7 7 7 7 7 7 7 7 8 9 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (1)
<Verbindung>
label 0
x[] 8 8 8 8 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 
y[] 11 12 13 14 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 15 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (2)
<Verbindung>
label 0
x[] 34 34 34 
y[] 15 14 13 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (3)
<Verbindung>
label 0
x[] 16 16 16 16 16 
y[] 11 12 13 14 15 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (4)
<Verbindung>
label in
x[] 16 15 14 13 12 11 10 9 8 8 8 8 8 
y[] 3 3 3 3 3 3 3 3 3 4 5 6 7 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (5)
<Verbindung>
label NIX_NIX_NIX
x[] 16 17 18 
y[] 7 7 7 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (6)
<Verbindung>
label NIX_NIX_NIX
x[] 23 22 
y[] 7 7 
enabled true
connectorType 0
<\\Verbindung>

verbindungCONTROL (0)
<Verbindung>
label gt
x[] 7 8 9 10 
y[] 18 18 18 18 
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
uniqueObjectIdentifier 307192937
x 8
y 9
parameter[] 401.0 12.0 50.0 0.0 0.0 0.5 -2.184785639712847E-6 11.999999999999998 0.0 -11.999999999999998 0.0 1.0 1.7976931348623157E308 0.0 0.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog U.1
coupledReferenceID[] 0 
copyCoupledReferenceID[] 0 
dxTxt 1.875
dyTxt 1.0

<\\ElementLK>

e (1)
<ElementLK>
labelAnfangsKnoten[] /0
labelEndKnoten[] /NIX_NIX_NIX
enabledShorted 1
typ 6
uniqueObjectIdentifier 116998914
x 16
y 9
parameter[] 0.01 0.0 0.01 1.0E7 4.198915005674664 0.04198915005674664 0.0 0.0 0.0 0.0 0.0 0.0 1.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 501
idStringDialog D.1
<Verluste>
verlustTyp 1
rON 0.01
uf 0.0
kON 0.0
kOFF 0.0
uSWnorm -1.0
Cosser 0.0
datnamGemesseneVerluste not_defined
lossFileHashValue 0
<\\Verluste>
dxTxt -1.75
dyTxt 2.5

<\\ElementLK>

e (2)
<ElementLK>
labelAnfangsKnoten[] /NIX_NIX_NIX
labelEndKnoten[] /NIX_NIX_NIX
enabledShorted 1
typ 2
uniqueObjectIdentifier -1229649930
x 20
y 7
parameter[] 2.0E-5 4.25 4.1989162098733 -4.957776986655911 -247888.84933279577 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
idStringDialog L.1
isNonlinear false
nonlinX[] 0.0 10.0 15.0 30.0 
nonlinY[] 5.0E-4 5.0E-4 1.5E-4 1.0E-4 
nonLinearCharHashValue 0
dxTxt -0.8125
dyTxt -2.25

<\\ElementLK>

e (3)
<ElementLK>
labelAnfangsKnoten[] /z1
labelEndKnoten[] /0
enabledShorted 1
typ 3
uniqueObjectIdentifier -453526184
x 27
y 13
parameter[] 2.0E-5 5.0 -0.7168706419417249 4.915787832400248 -7.08421216759975 -11.999999999999998 2.0E-5 2.0E-5 0.0 0.0 -0.7168706419417249 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog C.1
isNonlinear false
nonlinX[] 0.0 100.0 300.0 400.0 
nonlinY[] 1.0E-7 8.0E-8 1.2E-9 1.0E-9 
nonLinearCharHashValue 0
dxTxt -4.8125
dyTxt 0.5

<\\ElementLK>

e (4)
<ElementLK>
labelAnfangsKnoten[] /z1
labelEndKnoten[] /0
enabledShorted 1
typ 1
uniqueObjectIdentifier -2131230800
x 34
y 11
parameter[] 1.0 4.915787832400248 4.915787832400248 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog R.Last
dxTxt -2.8125
dyTxt 1.0

<\\ElementLK>

e (5)
<ElementLK>
labelAnfangsKnoten[] /in
labelEndKnoten[] /NIX_NIX_NIX
enabledShorted 1
typ 7
uniqueObjectIdentifier 727500737
x 16
y 5
parameter[] 1.0E7 0.01 1.0E7 1.2041989150056745E-6 12.041989150056745 0.0 3.0E-5 1.5E-5 0.0 0.0 0.0 0.0 1.0 
parameterString[] /GATE.1/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
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
dxTxt -3.5
dyTxt 0.25

<\\ElementLK>

e (6)
<ElementLK>
labelAnfangsKnoten[] /z1
labelEndKnoten[] /z1
enabledShorted 1
typ 1
uniqueObjectIdentifier -1651173459
x 27
y 9
parameter[] 0.0 0.0 0.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog R.4
dxTxt 1.125
dyTxt 1.0

<\\ElementLK>

e (7)
<ElementLK>
labelAnfangsKnoten[] /z1
labelEndKnoten[] /NIX_NIX_NIX
enabledShorted 1
typ 1
uniqueObjectIdentifier 1044681114
x 25
y 7
parameter[] 0.0 -4.198915881659104 -4.198915881659104E-9 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX/NIX_NIX_NIX
orientierung 504
idStringDialog R.L
dxTxt -0.625
dyTxt -1.4375

<\\ElementLK>

controlANZAHL 6

c (0)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /gt
enabledShorted 1
typ 4
uniqueObjectIdentifier -1678404099
x 5
y 18
parameter[] 404.0 1.0 100000.0 0.0 0.0 0.417 0.0 0.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog SIGNAL.1
dxTxt -1.0
dyTxt 2.0625
shiftLabelsIn[] 
shiftLabelsOut[] false 
<detail>
typQuelle 404
anteilDC 0.0
amplitudeAC 1.0
frequenz 100000.0
tastverhaeltnis 0.417
phase 0.0
datnamXY not_defined
externalDataFileHashValue 0
<\\detail>
<\\ElementCONTROL>

c (1)
<ElementCONTROL>
labelAnfangsKnoten[] /gt
labelEndKnoten[] 
enabledShorted 1
typ 6
uniqueObjectIdentifier 884353769
x 12
y 18
parameter[] 0.0 
parameterString[] /S.1/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog GATE.1
coupledReferenceID[] 727500737 
copyCoupledReferenceID[] 727500737 
dxTxt -1.5
dyTxt 1.5
shiftLabelsIn[] false 
shiftLabelsOut[] 
exportASCII_IndividualCONTROL()
<\\ElementCONTROL>

c (2)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /uOUT
enabledShorted 1
typ 1
uniqueObjectIdentifier -673981939
x 20
y 18
parameter[] 5.032120523973914 
parameterString[] /z1/0/0
nameOpt[] null
orientierung 503
idStringDialog VOLT.1
coupledReferenceID[] 0 
copyCoupledReferenceID[] 0 
dxTxt -4.0625
dyTxt 0.375
shiftLabelsIn[] 
shiftLabelsOut[] false 
<detail>
u[] 5.032120523973914 
<\\detail>
<\\ElementCONTROL>

c (3)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /uIN
enabledShorted 1
typ 1
uniqueObjectIdentifier -1563746041
x 20
y 19
parameter[] 12.0 
parameterString[] /in/0/0
nameOpt[] null
orientierung 503
idStringDialog VOLT.2
coupledReferenceID[] 0 
copyCoupledReferenceID[] 0 
dxTxt -4.1875
dyTxt 0.625
shiftLabelsIn[] 
shiftLabelsOut[] false 
<detail>
u[] 12.0 
<\\detail>
<\\ElementCONTROL>

c (4)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /iL1
enabledShorted 1
typ 2
uniqueObjectIdentifier -942935446
x 20
y 20
parameter[] 
parameterString[] /L.1/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog AMP.1
coupledReferenceID[] -1229649930 
copyCoupledReferenceID[] -1229649930 
dxTxt -3.9375
dyTxt 0.5
shiftLabelsIn[] 
shiftLabelsOut[] false 
exportASCII_IndividualCONTROL()
<\\ElementCONTROL>

c (5)
<ElementCONTROL>
labelAnfangsKnoten[] /uOUT/uIN/iL1/gt
labelEndKnoten[] 
enabledShorted 1
typ 5
uniqueObjectIdentifier -703889290
x 32
y 18
parameter[] 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] null
orientierung 503
idStringDialog SCOPE.1
dxTxt -1.5
dyTxt 0.19999999999999996
shiftLabelsIn[] false false false false 
shiftLabelsOut[] 
<detail>
tn 4
isShowName false
savedSignalNames[] /uOUT/uIN/iL1/gt
avgIndices[] 
avgValues[] 
<ScopeSettings>
isAntiAliasing true
<Diagram>

diagramType DiagramCurve
<xAxis>
axisType -111111114
noMinorTicks 2
isShowLabelsMaj false
isShowLabelsMin false
tickLengthMaj 0
tickLengthMin 0
userShowGridMaj true
userShowGridMin false
linStilGridNormal -3333330
linStilGridMinor -3333330
colorGridNormal -3444446
colorGridMinor -3444446
axisColor -3444440
axisStroke -3333331
axisCaption NIX_NIX_NIX
isAutoEnabled true
isUserScale 0.0 0.0
valueScale 0.0 3.0E-4
<\\xAxis>
<yAxis1>

axisType -111111114
noMinorTicks 2
isShowLabelsMaj true
isShowLabelsMin false
tickLengthMaj 10
tickLengthMin 5
userShowGridMaj true
userShowGridMin false
linStilGridNormal -3333330
linStilGridMinor -3333330
colorGridNormal -3444445
colorGridMinor -3444446
axisColor -3444440
axisStroke -3333330
axisCaption NIX_NIX_NIX
isAutoEnabled true
isUserScale 0.0 0.0
valueScale 0.0 1.0
<\\yAxis1>
<yAxis2>

axisType -111111114
noMinorTicks 2
isShowLabelsMaj true
isShowLabelsMin false
tickLengthMaj 10
tickLengthMin 5
userShowGridMaj true
userShowGridMin false
linStilGridNormal -3333330
linStilGridMinor -3333330
colorGridNormal -3444445
colorGridMinor -3444446
axisColor -3444440
axisStroke -3333330
axisCaption NIX_NIX_NIX
isAutoEnabled true
isUserScale 0.0 0.0
valueScale 0.0 1.0
<\\yAxis2>
<diagramSettings>

nameDiagram GRF 0
yWeightDiagram 0.5
<\\diagramSettings>
<CurveDiagram>

curveColor -3444443
curveLineStyle -3333330
showCurveSymbols false
symbolColor -3444444
symbolShape -838300
crvTransparency 0.9
fillDigitalColor -3444446
isFillDigitalCurves true
axisConnection 52
<\\CurveDiagram>
<CurveDiagram>

curveColor -3444452
curveLineStyle -3333330
showCurveSymbols false
symbolColor -3444444
symbolShape -838300
crvTransparency 0.9
fillDigitalColor -3444446
isFillDigitalCurves true
axisConnection 52
<\\CurveDiagram>
<CurveDiagram>

curveColor -3444440
curveLineStyle -3333330
showCurveSymbols false
symbolColor -3444444
symbolShape -838300
crvTransparency 0.9
fillDigitalColor -3444446
isFillDigitalCurves true
axisConnection 55
<\\CurveDiagram>
<CurveDiagram>

curveColor -3444440
curveLineStyle -3333330
showCurveSymbols false
symbolColor -3444444
symbolShape -838300
crvTransparency 0.9
fillDigitalColor -3444446
isFillDigitalCurves true
axisConnection 55
<\\CurveDiagram>
<\\Diagram>
<Diagram>

diagramType DiagramSignal
<xAxis>
axisType -111111114
noMinorTicks 2
isShowLabelsMaj false
isShowLabelsMin false
tickLengthMaj 0
tickLengthMin 0
userShowGridMaj true
userShowGridMin false
linStilGridNormal -3333330
linStilGridMinor -3333330
colorGridNormal -3444446
colorGridMinor -3444446
axisColor -3444440
axisStroke -3333331
axisCaption NIX_NIX_NIX
isAutoEnabled true
isUserScale 0.0 0.0
valueScale 0.0 3.0E-4
<\\xAxis>
<yAxis1>

axisType -111111114
noMinorTicks 2
isShowLabelsMaj true
isShowLabelsMin false
tickLengthMaj 10
tickLengthMin 5
userShowGridMaj true
userShowGridMin false
linStilGridNormal -3333330
linStilGridMinor -3333330
colorGridNormal -3444445
colorGridMinor -3444446
axisColor -3444440
axisStroke -3333330
axisCaption NIX_NIX_NIX
isAutoEnabled true
isUserScale 0.0 0.0
valueScale -1.0 1.0
<\\yAxis1>
<yAxis2>

axisType -111111114
noMinorTicks 2
isShowLabelsMaj true
isShowLabelsMin false
tickLengthMaj 10
tickLengthMin 5
userShowGridMaj true
userShowGridMin false
linStilGridNormal -3333330
linStilGridMinor -3333330
colorGridNormal -3444445
colorGridMinor -3444446
axisColor -3444440
axisStroke -3333330
axisCaption NIX_NIX_NIX
isAutoEnabled true
isUserScale 0.0 0.0
valueScale -1.0 1.0
<\\yAxis2>
<diagramSettings>

nameDiagram GRF 1
yWeightDiagram 0.0
<\\diagramSettings>
<CurveDiagram>

curveColor -3444440
curveLineStyle -3333330
showCurveSymbols false
symbolColor -3444444
symbolShape -838300
crvTransparency 0.9
fillDigitalColor -3444446
isFillDigitalCurves true
axisConnection 55
<\\CurveDiagram>
<CurveDiagram>

curveColor -3444440
curveLineStyle -3333330
showCurveSymbols false
symbolColor -3444444
symbolShape -838300
crvTransparency 0.9
fillDigitalColor -3444446
isFillDigitalCurves true
axisConnection 55
<\\CurveDiagram>
<CurveDiagram>

curveColor -3444440
curveLineStyle -3333330
showCurveSymbols false
symbolColor -3444444
symbolShape -838300
crvTransparency 0.9
fillDigitalColor -3444446
isFillDigitalCurves true
axisConnection 55
<\\CurveDiagram>
<CurveDiagram>

curveColor -3444440
curveLineStyle -3333330
showCurveSymbols false
symbolColor -3444444
symbolShape -838300
crvTransparency 0.9
fillDigitalColor -3444446
isFillDigitalCurves true
axisConnection 54
<\\CurveDiagram>
<\\Diagram>
<Diagram>

diagramType DiagramCurve
<xAxis>
axisType -111111114
noMinorTicks 2
isShowLabelsMaj true
isShowLabelsMin false
tickLengthMaj 10
tickLengthMin 5
userShowGridMaj true
userShowGridMin false
linStilGridNormal -3333330
linStilGridMinor -3333330
colorGridNormal -3444446
colorGridMinor -3444446
axisColor -3444440
axisStroke -3333330
axisCaption NIX_NIX_NIX
isAutoEnabled true
isUserScale 0.0 0.0
valueScale 0.0 3.0E-4
<\\xAxis>
<yAxis1>

axisType -111111114
noMinorTicks 2
isShowLabelsMaj true
isShowLabelsMin false
tickLengthMaj 10
tickLengthMin 5
userShowGridMaj true
userShowGridMin false
linStilGridNormal -3333330
linStilGridMinor -3333330
colorGridNormal -3444445
colorGridMinor -3444446
axisColor -3444440
axisStroke -3333330
axisCaption NIX_NIX_NIX
isAutoEnabled true
isUserScale 0.0 0.0
valueScale 0.0 1.0
<\\yAxis1>
<yAxis2>

axisType -111111114
noMinorTicks 2
isShowLabelsMaj true
isShowLabelsMin false
tickLengthMaj 10
tickLengthMin 5
userShowGridMaj true
userShowGridMin false
linStilGridNormal -3333330
linStilGridMinor -3333330
colorGridNormal -3444445
colorGridMinor -3444446
axisColor -3444440
axisStroke -3333330
axisCaption NIX_NIX_NIX
isAutoEnabled true
isUserScale 0.0 0.0
valueScale 0.0 1.0
<\\yAxis2>
<diagramSettings>

nameDiagram GRF 2
yWeightDiagram 0.5
<\\diagramSettings>
<CurveDiagram>

curveColor -3444440
curveLineStyle -3333330
showCurveSymbols false
symbolColor -3444444
symbolShape -838300
crvTransparency 0.9
fillDigitalColor -3444446
isFillDigitalCurves true
axisConnection 55
<\\CurveDiagram>
<CurveDiagram>

curveColor -3444440
curveLineStyle -3333330
showCurveSymbols false
symbolColor -3444444
symbolShape -838300
crvTransparency 0.9
fillDigitalColor -3444446
isFillDigitalCurves true
axisConnection 55
<\\CurveDiagram>
<CurveDiagram>

curveColor -3444441
curveLineStyle -3333330
showCurveSymbols false
symbolColor -3444444
symbolShape -838300
crvTransparency 0.9
fillDigitalColor -3444446
isFillDigitalCurves true
axisConnection 52
<\\CurveDiagram>
<CurveDiagram>

curveColor -3444440
curveLineStyle -3333330
showCurveSymbols false
symbolColor -3444444
symbolShape -838300
crvTransparency 0.9
fillDigitalColor -3444446
isFillDigitalCurves true
axisConnection 55
<\\CurveDiagram>
<\\Diagram>

<\\ScopeSettings>

<ScopeWindowSettings>

isAutoScaleWindowOn true
powerAnalCurIndices[] -1 -1 -1 
powerAnalVoltIndices[] -1 -1 -1 
windowWidth 741
windowHeight 615
saveScreenWidth 3840
saveScreenHeight 1200
saveScreenPosX 1985
saveScreenPosY 354
<\\ScopeWindowSettings>

<\\detail>

<\\ElementCONTROL>

<scopeSettings>
noInputSignals 4
<\\scopeSettings>
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


DtStor 2012-10-17
tDURATION 3.0E-4
bl 1378504800000
dt 5.0E-8
tPAUSE -1.0
T_pre -0.1
dt_pre 1.0E-6
solverType 0
path /home/andreas/GeckoCIRCUITSDistr/TutorialExercises/1_DCDC/A_Buck.ipes

dpix 16
fontSize 12
fontTyp Dialog.plain
fensterWidth 1000
fensterHeight 800
worksheetSize 1000x1000
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
FileVersion 160
UniqueFileId 2120803383
dataContainerSignals[] [] /uOUT/uIN/iL1/gt
=======================
 `;

export const RC_FILTER_IPES = `
verbindungLeistungskreisANZAHL 3
verbindungLK (0)
<Verbindung>
label NIX_NIX_NIX
x[] 4 5 6 7 8 
y[] 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (1)
<Verbindung>
label V_out
x[] 12 13 14 15 16 
y[] 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (2)
<Verbindung>
label GND
x[] 16 15 14 13 12 11 10 9 8 7 6 5 4 
y[] 10 10 10 10 10 10 10 10 10 10 10 10 10 
enabled true
connectorType 0
<\\Verbindung>

elementANZAHL 3

e (0)
<ElementLK>
labelAnfangsKnoten[] /NIX_NIX_NIX
labelEndKnoten[] /GND
enabledShorted 1
parentSheetIdentifier 0
typ 4
uniqueObjectIdentifier 31
x 4
y 8
parameter[] 401.0 10.0 50.0 0.0 0.0 0.5 0.0 10.0 10.0 0.0 0.0 1.0 1.7976931348623157E308 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog V_step
<\\ElementLK>

e (1)
<ElementLK>
labelAnfangsKnoten[] /NIX_NIX_NIX
labelEndKnoten[] /V_out
enabledShorted 1
parentSheetIdentifier 0
typ 1
uniqueObjectIdentifier 32
x 10
y 6
parameter[] 1000.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
idStringDialog R1
<\\ElementLK>

e (2)
<ElementLK>
labelAnfangsKnoten[] /V_out
labelEndKnoten[] /GND
enabledShorted 1
parentSheetIdentifier 0
typ 3
uniqueObjectIdentifier 33
x 16
y 8
parameter[] 1e-06 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX
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
dataContainerSignals[] /V_out
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
x[] 26 27 28 29 
y[] 10 10 10 10 
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
y 9
parameter[] 10.0 0.0 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX
orientierung 502
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

export const THREE_SCOPES_RLC_IPES = `
verbindungLeistungskreisANZAHL 4
verbindungLK (0)
<Verbindung>
label V_in
x[] 4 5 6 7 8 
y[] 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (1)
<Verbindung>
label n_mid
x[] 12 13 14 
y[] 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (2)
<Verbindung>
label V_out
x[] 18 19 20 21 22 
y[] 6 6 6 6 6 
enabled true
connectorType 0
<\\Verbindung>

verbindungLK (3)
<Verbindung>
label 0
x[] 22 21 20 19 18 17 16 15 14 13 12 11 10 9 8 7 6 5 4 
y[] 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 
enabled true
connectorType 0
<\\Verbindung>

verbindungControlANZAHL 4
verbindungCONTROL (0)
<Verbindung>
label v_in
x[] 28 29 30 
y[] 4 4 4 
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (1)
<Verbindung>
label v_R1
x[] 28 29 30 
y[] 8 8 8 
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (2)
<Verbindung>
label v_L1
x[] 28 29 30 
y[] 12 12 12 
enabled true
connectorType 1
<\\Verbindung>

verbindungCONTROL (3)
<Verbindung>
label v_C1
x[] 28 29 30 
y[] 16 16 16 
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
x 4
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
x 10
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
x 16
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
x 22
y 8
parameter[] 0.00001 0.0
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] /NIX_NIX_NIX/NIX_NIX_NIX
orientierung 503
idStringDialog C1
<\\ElementLK>

controlANZAHL 9

c (0)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_in
enabledShorted 1
typ 1
uniqueObjectIdentifier 2001
x 26
y 4
parameter[] 0.0
parameterString[] /V_in/0/0
nameOpt[] 
orientierung 503
idStringDialog VOLT_IN
<\\ElementCONTROL>

c (1)
<ElementCONTROL>
labelAnfangsKnoten[] /v_in
labelEndKnoten[] 
enabledShorted 1
typ 5
uniqueObjectIdentifier 2002
x 32
y 4
parameter[] 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] 
orientierung 503
idStringDialog SCOPE_VIN
<\\ElementCONTROL>

c (2)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_R1
enabledShorted 1
typ 1
uniqueObjectIdentifier 2003
x 26
y 8
parameter[] 0.0
parameterString[] /V_in/n_mid/0
nameOpt[] 
orientierung 503
idStringDialog VOLT_R1
<\\ElementCONTROL>

c (3)
<ElementCONTROL>
labelAnfangsKnoten[] /v_R1
labelEndKnoten[] 
enabledShorted 1
typ 5
uniqueObjectIdentifier 2004
x 32
y 8
parameter[] 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] 
orientierung 503
idStringDialog SCOPE_VR1
<\\ElementCONTROL>

c (4)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_L1
enabledShorted 1
typ 1
uniqueObjectIdentifier 2005
x 26
y 12
parameter[] 0.0
parameterString[] /n_mid/V_out/0
nameOpt[] 
orientierung 503
idStringDialog VOLT_L1
<\\ElementCONTROL>

c (5)
<ElementCONTROL>
labelAnfangsKnoten[] /v_L1
labelEndKnoten[] 
enabledShorted 1
typ 5
uniqueObjectIdentifier 2006
x 32
y 12
parameter[] 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] 
orientierung 503
idStringDialog SCOPE_VL1
<\\ElementCONTROL>

c (6)
<ElementCONTROL>
labelAnfangsKnoten[] 
labelEndKnoten[] /v_C1
enabledShorted 1
typ 1
uniqueObjectIdentifier 2007
x 26
y 16
parameter[] 0.0
parameterString[] /V_out/0/0
nameOpt[] 
orientierung 503
idStringDialog VOLT_C1
<\\ElementCONTROL>

c (7)
<ElementCONTROL>
labelAnfangsKnoten[] /v_C1
labelEndKnoten[] 
enabledShorted 1
typ 5
uniqueObjectIdentifier 2008
x 32
y 16
parameter[] 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] 
orientierung 503
idStringDialog SCOPE_VC1
<\\ElementCONTROL>

c (8)
<ElementCONTROL>
labelAnfangsKnoten[] /v_in/v_R1/v_L1/v_C1
labelEndKnoten[] 
enabledShorted 1
typ 5
uniqueObjectIdentifier 2009
x 40
y 10
parameter[] 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
nameOpt[] 
orientierung 503
idStringDialog SCOPE_ALL_VOLTAGES
<\\ElementCONTROL>

tDURATION 0.01
dt 1e-06
tPAUSE -1.0
T_pre -1.0
dt_pre 0.0
solverType 0
dpix 16
fontSize 12
fontTyp Dialog
worksheetSize 700_600
FileVersion 1
DtStor 2026-08-16
dataContainerSignals[] /v_in/v_R1/v_L1/v_C1
`;

export const EXAMPLES: CircuitExample[] = [
  {
    id: 'three-scopes',
    name: 'Multi-Scope & Multi-Signal RLC (All Component Voltages)',
    category: 'Analog & Filter',
    description: 'Individual Scopes for each component (v_in, v_R1, v_L1, v_C1) plus a Multi-Signal Scope (SCOPE_ALL_VOLTAGES) showing all voltages overlaid.',
    content: THREE_SCOPES_RLC_IPES,
  },
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
    description: 'Authentic tutorial buck converter (12V to ~5V) with freewheeling diode, controlled switch, LC filter, and load resistor.',
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
