GeckoSimulationProject
version 2.0
simulationParameters
dt §0§
tend §1§
dauer §2§
solverType 0
<\simulationParameters>

verbindungLeistungskreisANZAHL 12

verbindungLK (0)
<Verbindung>
label 1
zeigerAktuell 4
x[] 3 5 5 8
y[] 7 7 8 8
xPix[] 48 80 80 128
yPix[] 112 112 128 128
enabled true
connectorType 0
<\Verbindung>

verbindungLK (1)
<Verbindung>
label 2
zeigerAktuell 4
x[] 3 5 5 11
y[] 11 11 10 10
xPix[] 48 80 80 176
yPix[] 176 176 160 160
enabled true
connectorType 0
<\Verbindung>

verbindungLK (2)
<Verbindung>
label 0
zeigerAktuell 8
x[] 8 11 18 20 22 25 28 28
y[] 13 13 13 13 13 13 13 16
xPix[] 128 176 288 320 352 400 448 448
yPix[] 208 208 208 208 208 208 208 256
enabled true
connectorType 0
<\Verbindung>

verbindungLK (3)
<Verbindung>
label x1
zeigerAktuell 4
x[] 8 11 14 17
y[] 3 3 3 6
xPix[] 128 176 224 272
yPix[] 48 48 48 96
enabled true
connectorType 0
<\Verbindung>

verbindungLK (4)
<Verbindung>
label uOUT
zeigerAktuell 6
x[] 22 22 25 28 28 22
y[] 3 6 6 6 4 3
xPix[] 352 352 400 448 448 352
yPix[] 48 96 96 96 64 48
enabled true
connectorType 0
<\Verbindung>

verbindungLK (5)
<Verbindung>
label z1
zeigerAktuell 2
x[] 18 18
y[] 3 9
xPix[] 288 288
yPix[] 48 144
enabled true
connectorType 0
<\Verbindung>

verbindungLK (6)
<Verbindung>
label z2
zeigerAktuell 2
x[] 21 21
y[] 6 9
xPix[] 336 336
yPix[] 96 144
enabled true
connectorType 0
<\Verbindung>

verbindungLK (7)
<Verbindung>
label sw_mid
zeigerAktuell 2
x[] 28 28
y[] 8 10
xPix[] 448 448
yPix[] 128 160
enabled true
connectorType 0
<\Verbindung>

ElementLKAnzahl 14

e (0)
<ElementLK>
labelAnfangsKnoten[] /1
labelEndKnoten[] /2
enabledShorted 1
typ 4
uniqueObjectIdentifier 1001
x 3
y 9
parameter[] 402.0 §3§ §4§ 0.0 0.0 0.5 0.0 0.0 0.0 0.0 0.0 
parameterString[] /uN/NIX_NIX_NIX/0
orientierung 503
idStringDialog U.1
<\ElementLK>

e (1)
<ElementLK>
labelAnfangsKnoten[] /1
labelEndKnoten[] /x1
enabledShorted 1
typ 6
uniqueObjectIdentifier 1002
x 8
y 5
parameter[] 1.0E7 0.7 0.005 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0 
orientierung 501
idStringDialog D.1
<\ElementLK>

e (2)
<ElementLK>
labelAnfangsKnoten[] /2
labelEndKnoten[] /x1
enabledShorted 1
typ 6
uniqueObjectIdentifier 1003
x 11
y 5
parameter[] 1.0E7 0.7 0.005 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0 
orientierung 501
idStringDialog D.2
<\ElementLK>

e (3)
<ElementLK>
labelAnfangsKnoten[] /0
labelEndKnoten[] /1
enabledShorted 1
typ 6
uniqueObjectIdentifier 1004
x 8
y 11
parameter[] 1.0E7 0.7 0.005 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0 
orientierung 501
idStringDialog D.3
<\ElementLK>

e (4)
<ElementLK>
labelAnfangsKnoten[] /0
labelEndKnoten[] /2
enabledShorted 1
typ 6
uniqueObjectIdentifier 1005
x 11
y 11
parameter[] 1.0E7 0.7 0.005 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0 
orientierung 501
idStringDialog D.4
<\ElementLK>

e (5)
<ElementLK>
labelAnfangsKnoten[] /x1
labelEndKnoten[] /z1
enabledShorted 1
typ 2
uniqueObjectIdentifier 1006
x 16
y 3
parameter[] §5§ 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 
orientierung 502
idStringDialog L.1
<\ElementLK>

e (6)
<ElementLK>
labelAnfangsKnoten[] /x1
labelEndKnoten[] /z2
enabledShorted 1
typ 2
uniqueObjectIdentifier 1007
x 19
y 6
parameter[] §6§ 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 
orientierung 502
idStringDialog L.2
<\ElementLK>

e (7)
<ElementLK>
labelAnfangsKnoten[] /z1
labelEndKnoten[] /0
enabledShorted 1
typ 7
uniqueObjectIdentifier 1008
x 18
y 11
parameter[] 0.0 0.01 1.0E6 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 
orientierung 503
idStringDialog S.1
<\ElementLK>

e (8)
<ElementLK>
labelAnfangsKnoten[] /z2
labelEndKnoten[] /0
enabledShorted 1
typ 7
uniqueObjectIdentifier 1009
x 21
y 11
parameter[] 0.0 0.01 1.0E6 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 
orientierung 503
idStringDialog S.2
<\ElementLK>

e (9)
<ElementLK>
labelAnfangsKnoten[] /z1
labelEndKnoten[] /uOUT
enabledShorted 1
typ 6
uniqueObjectIdentifier 1010
x 20
y 3
parameter[] 1.0E7 0.7 0.01 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0 
orientierung 502
idStringDialog D.5
<\ElementLK>

e (10)
<ElementLK>
labelAnfangsKnoten[] /z2
labelEndKnoten[] /uOUT
enabledShorted 1
typ 6
uniqueObjectIdentifier 1011
x 23
y 6
parameter[] 1.0E7 0.7 0.01 1.0E7 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0 
orientierung 502
idStringDialog D.6
<\ElementLK>

e (11)
<ElementLK>
labelAnfangsKnoten[] /uOUT
labelEndKnoten[] /0
enabledShorted 1
typ 3
uniqueObjectIdentifier 1012
x 22
y 8
parameter[] §7§ §8§ 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 
orientierung 503
idStringDialog C.1
<\ElementLK>

e (12)
<ElementLK>
labelAnfangsKnoten[] /uOUT
labelEndKnoten[] /0
enabledShorted 1
typ 1
uniqueObjectIdentifier 1013
x 25
y 8
parameter[] §9§ 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 
orientierung 503
idStringDialog R.1
<\ElementLK>

e (13)
<ElementLK>
labelAnfangsKnoten[] /uOUT
labelEndKnoten[] /sw_mid
enabledShorted 1
typ 7
uniqueObjectIdentifier 1014
x 28
y 6
parameter[] 0.0 0.005 1.0E6 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 
orientierung 503
idStringDialog S.LOAD
<\ElementLK>

e (14)
<ElementLK>
labelAnfangsKnoten[] /sw_mid
labelEndKnoten[] /0
enabledShorted 1
typ 1
uniqueObjectIdentifier 1015
x 28
y 12
parameter[] §10§ 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 
orientierung 503
idStringDialog R.2
<\ElementLK>

ElementCONTROLAnzahl 5

c (0)
<ElementCONTROL>
labelAnfangsKnoten[] /uOUT
labelEndKnoten[] /gate1/gate2/gate_load/v_out_telemetry/duty_telemetry
enabledShorted 1
parentSheetIdentifier 0
typ 61
uniqueObjectIdentifier 2001
x 16
y 30
parameter[] 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 
parameterString[] /NIX_NIX_NIX/NIX_NIX_NIX/0
orientierung 503
idStringDialog CTRL_MCU
anzXIN 1
anzYOUT 5
showName true
<sourceCode>
§11§
<\sourceCode>
<staticCode>
<\staticCode>
<importCode>
<\importCode>
<staticVariables>
§12§
<\staticVariables>
<\ElementCONTROL>

c (1)
<ElementCONTROL>
labelAnfangsKnoten[] /gate1
labelEndKnoten[] /NIX_NIX_NIX
enabledShorted 1
typ 6
uniqueObjectIdentifier 2002
x 24
y 30
parameter[] 0.0
coupledReferenceID[] 1008
orientierung 503
idStringDialog GATE.1
<\ElementCONTROL>

c (2)
<ElementCONTROL>
labelAnfangsKnoten[] /gate2
labelEndKnoten[] /NIX_NIX_NIX
enabledShorted 1
typ 6
uniqueObjectIdentifier 2003
x 24
y 31
parameter[] 0.0
coupledReferenceID[] 1009
orientierung 503
idStringDialog GATE.2
<\ElementCONTROL>

c (3)
<ElementCONTROL>
labelAnfangsKnoten[] /gate_load
labelEndKnoten[] /NIX_NIX_NIX
enabledShorted 1
typ 6
uniqueObjectIdentifier 2004
x 24
y 32
parameter[] 0.0
coupledReferenceID[] 1014
orientierung 503
idStringDialog GATE.LOAD
<\ElementCONTROL>

c (4)
<ElementCONTROL>
labelAnfangsKnoten[] /NIX_NIX_NIX
labelEndKnoten[] /uOUT
enabledShorted 1
typ 1
uniqueObjectIdentifier 2005
x 10
y 30
parameter[] 0.0
coupledReferenceID[] 1013
orientierung 503
idStringDialog VOLT.OUT
<\ElementCONTROL>

verbindungCONTROL (0)
<Connection>
label NIX_NIX_NIX
x[] 12 14
y[] 30 30
enabledShorted 1
parentSheetIdentifier 0
connectorType 1
<\Connection>

verbindungCONTROL (1)
<Connection>
label gate1
x[] 18 22
y[] 30 30
enabledShorted 1
parentSheetIdentifier 0
connectorType 1
<\Connection>

verbindungCONTROL (2)
<Connection>
label gate2
x[] 18 22
y[] 31 31
enabledShorted 1
parentSheetIdentifier 0
connectorType 1
<\Connection>

verbindungCONTROL (3)
<Connection>
label gate_load
x[] 18 22
y[] 32 32
enabledShorted 1
parentSheetIdentifier 0
connectorType 1
<\Connection>
