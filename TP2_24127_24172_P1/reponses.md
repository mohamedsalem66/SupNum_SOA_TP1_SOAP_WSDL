# TP1 — SOAP et WSDL : réponses

SupNum · Architecture SOA & Web Services · 2026–2027

Résultats relevés avec Metro (jaxws-rt 4.0.2), serveur sur `http://localhost:8080/bib/EmpruntService`, requêtes envoyées avec curl.

---

## Partie A — Code-first

### Étape 1 — Créer et lancer le service

**1. Quelle annotation fait de `EmpruntService` un service web ? Laquelle fait de `LivreIndisponibleException` une erreur du contrat ?**

- `@WebService` (jakarta.jws) sur la classe `EmpruntService` en fait un service web.
- `@WebFault(name = "LivreIndisponible", targetNamespace = "http://supnum.mr/bib")` sur `LivreIndisponibleException` en fait une faute déclarée dans le contrat. Comme `emprunterLivre` déclare `throws LivreIndisponibleException`, le WSDL contient un `<fault>` pour cette opération.

**2. Pourquoi les classes de données doivent-elles avoir un constructeur vide ?**

JAXB transforme le XML en objets Java. Il crée d'abord l'objet avec le constructeur sans argument, puis remplit ses champs. Sans ce constructeur, JAXB ne peut pas instancier `Emprunt` ou `LivreIndisponibleInfo`.

### Étape 2 — Lire le WSDL généré

**1. URL du service, opérations et messages**

- URL : `http://localhost:8080/bib/EmpruntService` (`soap:address` de `service/port EmpruntPort`).
- `emprunterLivre` : entrée `emprunterLivre` (`idAdherent`, `isbn`), sortie `emprunterLivreResponse` (`emprunt` avec `numero`, `dateRetour`), faute `LivreIndisponibleException` (élément `LivreIndisponible`).
- `rendreLivre` : entrée `rendreLivre` (`numeroEmprunt`), sortie `rendreLivreResponse` (`penalite`).

**2. Style, use et version de SOAP**

`style="document"`, `use="literal"`. C'est du SOAP 1.1 : le binding utilise `http://schemas.xmlsoap.org/wsdl/soap/` avec `transport="http://schemas.xmlsoap.org/soap/http"`.

**3. Types XSD de `isbn` et `dateRetour`. La règle de l'ISBN apparaît-elle ?**

Les deux sont `xs:string` (avec `minOccurs="0"`), car ce sont des `String` en Java. La règle de format de l'ISBN n'apparaît pas : elle n'existe que dans le code Java (`Pattern`). Un client ne peut pas la connaître en lisant le contrat.

**4. Les paramètres portent-ils un préfixe d'espace de noms ?**

Non. Seul l'élément d'opération est qualifié (`<bib:emprunterLivre>`). Les enfants `<idAdherent>` et `<isbn>` sont sans espace de noms : le schéma généré n'a pas `elementFormDefault="qualified"`.

**5. Où voit-on HTTP, XML, SOAP dans l'onglet Raw ? Rôle de chacun et du WSDL**

- **HTTP** : la ligne `POST /bib/EmpruntService HTTP/1.1` et les en-têtes (`Host`, `Content-Type: text/xml`, `SOAPAction`) ; dans la réponse, `HTTP/1.1 200 OK` ou `500`. HTTP transporte la requête et la réponse.
- **XML** : le corps du message (balises, espaces de noms). XML est le format des données.
- **SOAP** : la structure `Envelope` / `Header` / `Body` (/ `Fault`) dans le XML. SOAP définit l'enveloppe et les règles de traitement et d'erreur.
- **WSDL** : il ne circule pas dans l'appel ; c'est le contrat qui décrit où appeler (`service`), comment emballer (`binding`), quelles opérations (`portType`) et quelles données (`message`, `types`). SoapUI s'en sert pour générer la requête.

### Étape 3 — Envoyer des messages SOAP

Fichier `xml/01-emprunter.xml` → **HTTP/1.1 200** :

```xml
<ns2:emprunterLivreResponse xmlns:ns2="http://supnum.mr/bib">
  <emprunt><numero>E-2026-0812</numero><dateRetour>2026-12-10</dateRetour></emprunt>
</ns2:emprunterLivreResponse>
```

**1. Pourquoi POST, même pour lire une information ?**

Avec le binding SOAP/HTTP, l'appel est une enveloppe XML placée dans le corps de la requête HTTP ; il faut donc une méthode qui transporte un corps : POST. L'opération appelée est désignée par le contenu du Body, pas par la méthode HTTP. Seul le WSDL se lit en GET (`?wsdl`).

**2. Le bloc `jeton` du Header a-t-il empêché le traitement ?**

Non, la réponse est 200. Le bloc n'a pas `mustUnderstand="1"` : un nœud SOAP peut ignorer un bloc d'en-tête qu'il ne connaît pas. Aucun code du service ne lit `jeton`.

**3. Que répond le service au message SOAP 1.2 ?**

HTTP **500** avec une Fault SOAP 1.1 :

```
faultcode   : S:VersionMismatch
faultstring : Impossible de créer le message SOAP. Attente de Envelope dans l'espace de noms
              http://schemas.xmlsoap.org/soap/envelope/, mais http://www.w3.org/2003/05/soap-envelope obtenu
```

Le binding lu à l'étape 2 est SOAP 1.1 : le service n'accepte que l'enveloppe `http://schemas.xmlsoap.org/soap/envelope/`. Une autre version d'enveloppe donne `VersionMismatch`, et la Fault est renvoyée en 1.1.

**4. Les cinq erreurs et leur correction**

| # | Erreur | Correction |
|---|--------|-----------|
| 1 | `<soap:envelope>` en minuscule (ne correspond pas à `</soap:Envelope>`) | `<soap:Envelope>` |
| 2 | `xmlns:bib="http://supnum.mr/bibliotheque"` | `xmlns:bib="http://supnum.mr/bib"` |
| 3 | `<bib:numero>` : mauvais nom et qualifié | `<numeroEmprunt>` sans préfixe |
| 4 | `</rendreLivre>` ne ferme pas `<bib:rendreLivre>` | `</bib:rendreLivre>` |
| 5 | `soap:Header` placé après `soap:Body` | `soap:Header` avant `soap:Body` |

Message corrigé (réponse **200**, `<penalite>0</penalite>`) :

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
               xmlns:bib="http://supnum.mr/bib">
  <soap:Header><bib:jeton>abc123</bib:jeton></soap:Header>
  <soap:Body>
    <bib:rendreLivre>
      <numeroEmprunt>E-2026-0812</numeroEmprunt>
    </bib:rendreLivre>
  </soap:Body>
</soap:Envelope>
```

Comportement observé de chaque erreur :

- Erreurs 1 et 4 : XML invalide → Fault `Client` (« balise XML inattendue ; attendue : …Envelope, obtenue : …envelope »).
- Erreur 2 : Fault `Client` « Méthode de répartition introuvable pour {http://supnum.mr/bibliotheque}rendreLivre ».
- **Erreurs silencieuses :**
  - **Erreur 5 (Header après Body)** : réponse 200, aucune erreur. Metro lit le Body, trouve l'opération et ne vérifie pas l'ordre des éléments de l'enveloppe.
  - **Erreur 3 (`bib:numero`)** : le service ne valide pas le message selon le schéma, donc l'élément inconnu est ignoré et `numeroEmprunt` vaut `null`. Avant l'étape 4, `rendreLivre` renvoyait toujours `0` : réponse 200, sans erreur. Depuis l'étape 4, on obtient la Fault `Client` « Numéro d'emprunt inconnu : null ».

### Étape 4 — Provoquer et construire des Faults

**1. Cas 1 et 2 : faultcode, faultstring, code HTTP**

| Cas | faultcode | faultstring | HTTP |
|-----|-----------|-------------|------|
| ISBN `978-2-10` | `S:Client` | `ISBN mal formé : 978-2-10` | 500 |
| ISBN `978-2-10-074444-4` | `S:Server` | `Livre indisponible` | 500 |

Le numéro invalide de `rendreLivre` (étape 4.4, `INCONNU`) donne : `S:Client`, `Numéro d'emprunt inconnu : INCONNU`, HTTP 500.

**2. Que contient `<detail>` dans le cas 2 ? Où est-ce décrit dans le WSDL ?**

```xml
<detail>
  <ns2:LivreIndisponible xmlns:ns2="http://supnum.mr/bib">
    <isbn>978-2-10-074444-4</isbn>
    <retourPrevu>2026-12-02</retourPrevu>
  </ns2:LivreIndisponible>
</detail>
```

Dans le WSDL : l'opération `emprunterLivre` du `portType` déclare `<fault message="tns:LivreIndisponibleException">` ; ce message a une partie `element="tns:LivreIndisponible"`. Cet élément est défini dans le schéma (`?xsd=1`) avec le type `livreIndisponibleInfo` (`isbn`, `retourPrevu`).

**3. Quel code JAX-WS a-t-il choisi pour le livre indisponible ? Client ou serveur ? Votre choix ?**

JAX-WS a choisi `Server`, car c'est le code par défaut pour une exception métier (`@WebFault`). Ce n'est pourtant pas une panne du serveur : la requête est bien formée, mais le livre n'est pas disponible. C'est un refus métier lié à la demande du client. J'aurais choisi `Client` : renvoyer la même requête tout de suite échouera de la même façon.

**4. `mustUnderstand="1"` et `"0"`**

- `"1"` : HTTP 500, `faultcode S:MustUnderstand`, `faultstring MustUnderstand headers:[{http://supnum.mr/bib}jeton] are not understood`. Le service ne sait pas traiter `jeton`, il doit donc refuser le message avant d'exécuter l'opération.
- `"0"` : le bloc est facultatif et ignoré ; le traitement continue normalement (l'opération `emprunterLivre` s'exécute, comme sans l'attribut).

**5. Différence entre la Fault 1.2 (`03-fault-sender-12.xml`) et les Faults 1.1 reçues**

| SOAP 1.1 (reçue) | SOAP 1.2 (écrite) |
|------------------|-------------------|
| Espace de noms `http://schemas.xmlsoap.org/soap/envelope/` | `http://www.w3.org/2003/05/soap-envelope` |
| `<faultcode>` texte simple | `<soap:Code><soap:Value>` (sous-codes possibles) |
| `Client` / `Server` | `Sender` / `Receiver` |
| `<faultstring>` sans langue | `<soap:Reason><soap:Text xml:lang="fr">` (plusieurs langues possibles) |
| `<detail>` non qualifié | `<soap:Detail>` qualifié |
| Éléments enfants non qualifiés | Éléments enfants qualifiés `soap:` |

---


