1.Szczegółowy opis implementacji
Protokuł komunikacji pomiędzy węzłami został oparty o protokół TCP.
Zawiera on 5 rodzaji komunikatów

-PORT-
Komunikat ten wysyłany jest do węzła z którym się połączyliśmy aby 
ten wiedział do jakich węzłów ma dostęp. Po odebraniu takiego komunikatu węzeł zapisuje 
do kontenera adres IP oraz port podany w komunikacie. 
Budowa: PORT <adres IP węzła> <port węzła>


-ALLOCATED-
Komunikat wysyłany w momencie poprawnej alokacji zasobów
zawierający typ zasobu oraz jego ilość wraz z adresem IP oraz portem węzła
na którym zasób został zaalokowany.
Budowa: ALLOCATED <typ zasobu> <ilosc zasobu> <adres IP węzła> <port węzła>


-MISSING-
Komunikat wysyłany w momencie kiedy nie ma odpowiedniego typu zasobu 
lub kiedy jest za mało zasobów danego typu w węźle. Komunikat
zawiera typ zasobu wraz z brakującą ilością którą trzeba gdzieś zaalokować
oraz adres IP i port węzła na którym brakowało zasobów.
Budowa: MISSING <typ zasobu> <ilosc brakujacego zasobu> <adres IP węzła> <port węzła>


-FAILED-
Komunikat wysyłany w momnecie kiedy nie da się zaalokować zasobów
Budowa: FAILED


-ALLGOOD-
Komunikat wysyłany w momencie poprawnej alokacji wszystkich zasobów.
Zawiera adres IP oraz port węzła który rozsyła komunikat ALLGOOD.
Budowa: ALLGOOD <adres IP węzła> <port węzła>


-Sposób działania-

W momencie tworzenia węzła wysyła on komunikat PORT do węzła do którego jest podłączony. Następnie po odebraniu połączenia od klienta dokonuje on próby 
alokacji zasobów. Jeśli zasobów brakuje wysyła on komunikat MISSING do węzłow do których ma dostęp. Węzeł który otrzymuje komunikat MISSING
dokonuje próby alokacji zasobów. Jeżeli taka próba się powiedzie odsyła on komunikat ALLOCATED. Jeżeli jednak wciąż brakuje zasobów rozsyła on dalej
komunikat MISSING do wezłow do których ma dostęp. Jeżeli węzeł który otrzyma komunikat MISSING nie może dokonać alokacji zasobów a nie ma już węzła do 
którego może się udać, to wysyła komunikat FAILED. Węzeł który otrzyma komunikat FAILED przesyła do dalej chyba że jest to węzeł do którego jest podłączony 
klient. W takim wypadku komunikat FAILED jest wysyłant bezpośrednio do klienta. Jeśli węzęł który wysłał MISSING otrzyma z powrotem komunikat ALLOCATED odsyła go dalej
chyba że jest to węzeł do którego jest podłączony klient. W takim wypadku komunikat ALLOCATED jest wysyłąny bezpośrednio do klienta. 


2.Jak skompilowac i zainstalowac
  Kompilacj:
  javac NetworkNode.java
  Uruchomienie:
  java NetworkNode -ident <identifier> -tcpport <tcpPort> -gateway <name>:<port> <resource list>
  lub:
  java NetworkNode -ident <identifier> -tcpport <tcpPort> <resource list>

3.Co zostalo zaimplementowane
Implementacja węzła sieci umożliwiającą organizacje sieci.
Implementacja funkcjonalności pozwalającej na odbieranie żądań od klientów.
Implementacja funkcjonalności pozwalającej na alokacje zasobów o ile to możliwe.
Implementacja funkcjonalnosci pozwalającej na alokacje w dowolnym węźle.
Implementacja własnego protokołu komunikacji opartego o TCP służącej do komunikacji pomiędzy węzłami.
Implementacja węzłow pozwalająca na ułożenie ich w dowolnej topologi.

4.Co nie dziala
Węzły nie zapamietują dla jakich klientów zaalokowały zasoby.
Scenariusz w którym węzeł utwrzony jako pierwszy musi przesłać dalej komunikat MISSING działa nieprawidłowo i wysyła komunikat FAILED.
W przypadku kiedy mamy więcej niż dwa węzły podczas alokacji zasbów węzły pośredniczące w operacji wykonują alokacje ale o tym nie informują przez co Klient może nie otrzymać pełnego raportu.
