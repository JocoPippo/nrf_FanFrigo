# Introduzione
In estate il frigo trivalente soffre molto a causa delle alte temperature che non consentono un scambio termico adeguato della serpentina di raffreddamente.
In commercio esistono soluzioni ma queste sono pienamente soddisfacenti.
Così ho deciso di realizzare una soluzione che permetesse di:
- regolare la velocità delle ventole in base alla temperature del vano;
- monitorare e controllare da remoto lo stato della board;
- la seconda ventola, sen non forzata da applicativo, viene attivata quando la prima supera la soglia del 70% di utilizzo così da ridurre la rumorosità;
- non pesare sui consumi elettrici quando non necessario.

La definizione dell'hardware la potete trovare qui: https://github.com/JocoPippo/nrf_FanFrigo. Utilizza una board custom, un sensore di temperatura DS18B20 e due ventole da 120x120mm (ma possono essere sostituite in base alla dimensione dello spazio utile a vostra disposizione).

L'applicazione di gestione su android la potete trovare qui: https://github.com/JocoPippo/nrf_FanFrigo. Basata sulla demo [Nrf BLINKY](https://github.com/nordicsemi/Android-nRF-Blinky) della nordic semiconductor è stata customizzata per il controllo e gestione della board custom. 

# Dettagli
Il firmware per la [board](https://github.com/JocoPippo/nrf_FanFrigo) si basa sul modulo [E73-2G4M04S1B](https://www.cdebyte.com/products/E73-2G4M04S1B/2) che utilizza il chip nfr52832 della nordic semiconductor, un ultra low power BLE soc che consente di controllare le ventole da remoto e, se in stand by mode consuma solo 3.5uA e circa 70uA in normal mode senza attivazione delle ventole.
Il che non rappresenta un problema per la batteria nei periodi di fermo prolungato del nostro mezzo.

# Setup
Il codice è scritto seguendo le linee guida del produttore qui troverete la descrizione per il [setup ambiente](https://academy.nordicsemi.com/courses/nrf-connect-sdk-fundamentals/lessons/lesson-1-nrf-connect-sdk-introduction/topic/exercise-1-1/). 
Allo stato della scrittura del codice le versioni dell'SDK e della toolchain è: 3.3.0.

Una volta importato il progetto dentro Visual Studio Code è necessario creare la configurazione di build selezionando "+Add build configuration" nella sezione "APPLICATIONS" di "NRF CONNECT".
- Nella sezione "Board Target" selezionare "Nordic SoC" e scegliere "ebyte_e73_tbb/nrf53832".
- Nella sezione "Base Devicetree Overlays" selezionare "boards/ebyte_e73_tbab_nrf52832.overlay"
- Nella sezione "Extra Devicetree Overlays" selezionare "arduino_serial.overlay"
A questo punto è possibile customizzare altre impostazione per uso specifico per il debugging o altro. Infine cliccare sul bottone "Generate and Build" per creare generare tutti le dipendenze e creare il file da flashare.

# Considerazioni
Attualmente il bluethooth è configurato con cifratura e password fissa definita nel codice, questo è stato necessario poichè non è presito alcun dispositivo di output per stampare la password di sessione.

L'appicativo di gestione consente il cambio dei parametri tra cui la temperatura di soglia e la password, che attualmente durano solo filno al ciclo di disconnessione dell'alimentazione. 
E' prevista la possibilità di salvare le impostazioni di sessione alliinterno della flash ma per il momento è stata commentata perchè salvare la password in modo definitivo avrebbe richiesto il re-flash in caso la si debba resettare.

# Sviluppi futuri 
Assieme alla prossima revisione hardware verranno aggiunte:
- la possibilità di monitorare la velocità delle ventole al fine di essere confidenti che non ci sia un impedimento meccanico alla rotazione;
- il monitoraggio della tesione della battaria durante l'azionamento della ventola principale.


