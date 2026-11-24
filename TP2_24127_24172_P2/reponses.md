# TP1 — SOAP et WSDL : réponses

SupNum · Architecture SOA & Web Services · 2026–2027

Réponses préparées à partir du projet fourni ; les résultats d’exécution sont à vérifier sur la machine du binôme.

## Partie B — Contract-first

### Étape 5 — Générer le code depuis le WSDL

**1. Opération, message de fault et action SOAP**

- Opération : `emprunterLivre` (portType `EmpruntPortType`).
- Message de fault : `bib:LivreIndisponibleFault` (fault `LivreIndisponible`, élément `bib:LivreIndisponible`).
- Action SOAP : `soapAction="http://supnum.mr/bib/emprunterLivre"`, donc l'en-tête `SOAPAction: "http://supnum.mr/bib/emprunterLivre"`.

**2. Signature de `emprunterLivre` dans `EmpruntPortType`**

```java
@WebMethod(action = "http://supnum.mr/bib/emprunterLivre")
@WebResult(name = "emprunt", targetNamespace = "http://supnum.mr/bib")
public EmpruntType emprunterLivre(
    @WebParam(name = "idAdherent", targetNamespace = "http://supnum.mr/bib") String idAdherent,
    @WebParam(name = "isbn", targetNamespace = "http://supnum.mr/bib") String isbn)
    throws LivreIndisponibleFault;
```

**3. Type Java généré pour `dateRetour`. Comparaison avec la partie A**

`javax.xml.datatype.XMLGregorianCalendar`, annoté `@XmlSchemaType(name = "date")`, car le WSDL déclare `xs:date`. Dans la partie A, c'était un `String`, devenu `xs:string` dans le WSDL généré.

**4. Nom de la classe d'exception générée et origine du nom**

`LivreIndisponibleFault`. Le nom vient du `wsdl:message name="LivreIndisponibleFault"` utilisé par le `wsdl:fault` de l'opération. Son contenu (`getFaultInfo()`) est de type `LivreIndisponible`, généré à partir de l'élément XSD `LivreIndisponible`.

Fichiers générés dans `target/generated-sources/wsimport/mr/supnum/bib/contrat/` : `EmpruntPortType`, `EmpruntService`, `EmpruntType`, `EmprunterLivre`, `EmprunterLivreResponse`, `LivreIndisponible`, `LivreIndisponibleFault`, `ObjectFactory`, `package-info`.

### Étape 6 — Implémenter et comparer

**1. Le WSDL publié est-il celui fourni ? Contient-il le motif de l'ISBN et `xs:date` ?**

Oui. Metro publie le fichier fourni (préfixes `wsdl:`/`bib:` d'origine, `EmpruntSoapBinding`, `EmpruntPortType`), seule l'adresse `soap:address` est mise à jour. On y retrouve `<xs:pattern value="97[89]-\d-\d{2,5}-\d{2,6}-\d"/>` et `type="xs:date"` pour `dateRetour` et `retourPrevu`.

**2. Avez-vous dû modifier `01-emprunter.xml` ? Pourquoi ?**

Oui. Le schéma déclare `elementFormDefault="qualified"` : les paramètres doivent être dans l'espace de noms `http://supnum.mr/bib`. Il faut écrire :

```xml
<bib:emprunterLivre>
  <bib:idAdherent>A-117</bib:idAdherent>
  <bib:isbn>978-2-07-036822-8</bib:isbn>
</bib:emprunterLivre>
```

Avec le fichier de la partie A (paramètres sans préfixe) :
- sans `@SchemaValidation` : les paramètres sont ignorés, `isbn` vaut `null` → Fault `Server` « Cannot invoke "String.equals(Object)" because "isbn" is null » ;
- avec `@SchemaValidation` : Fault `Client` « cvc-complex-type.2.4.a: Invalid content was found starting with element 'idAdherent'. One of '{"http://supnum.mr/bib":idAdherent}' is expected. »

On envoie aussi l'en-tête `SOAPAction: "http://supnum.mr/bib/emprunterLivre"`. Avec la version corrigée : HTTP 200, `E-2026-0812`. Avec l'ISBN `978-2-10-074444-4` : HTTP 500, `S:Server`, « Livre indisponible », detail `<LivreIndisponible xmlns="http://supnum.mr/bib"><isbn>978-2-10-074444-4</isbn><retourPrevu>2026-12-02</retourPrevu></LivreIndisponible>`.

**3. ISBN `978-2-10` sans et avec `@SchemaValidation`**

- **Sans** : il n'est pas refusé. HTTP 200 et un emprunt `E-2026-0812` est créé : le motif est dans le WSDL, mais Metro ne valide pas les messages par défaut, et l'implémentation ne vérifie pas le format.
- **Avec** : il est refusé avant d'arriver au code. HTTP 500, Fault :

```
faultcode   : S:Client
faultstring : org.xml.sax.SAXParseException; cvc-pattern-valid: Value '978-2-10' is not facet-valid
              with respect to pattern '97[89]-\d-\d{2,5}-\d{2,6}-\d' for type 'IsbnType'.
```

**4. Retirer `wsdlLocation` : qu'est-ce qui change dans le WSDL publié ?**

Metro ne publie plus le fichier fourni : il régénère un WSDL à partir des classes Java (comme en code-first).
- Le schéma est déplacé dans un fichier séparé (`<xsd:import … schemaLocation="…?xsd=1"/>`).
- **Le motif de l'ISBN disparaît** : `isbn` devient `xs:string`, et le type `IsbnType` n'existe plus.
- `xs:date` est conservé (grâce à `@XmlSchemaType` sur `XMLGregorianCalendar`), ainsi que `elementFormDefault="qualified"`.
- Les noms changent : message d'entrée `emprunterLivre` au lieu de `emprunterLivreRequest`, binding `EmpruntPortBinding` au lieu de `EmpruntSoapBinding`, fault nommée `LivreIndisponibleFault` au lieu de `LivreIndisponible`, et ajout d'actions `wsam:Action`.

Le contrat publié n'est donc plus exactement celui fourni aux clients.

---

## Conclusion — Choisir et rendre

**1. Deux différences observées entre le WSDL de la partie A et celui de la partie B**

1. **Types** : en A, `isbn` et `dateRetour` sont `xs:string` sans contrainte ; en B, `isbn` est `IsbnType` avec `xs:pattern` et `dateRetour` est `xs:date`.
2. **Qualification** : en A, les paramètres ne sont pas qualifiés (`<isbn>`) ; en B, `elementFormDefault="qualified"` impose `<bib:isbn>`.

Autres différences : `soapAction=""` en A contre `http://supnum.mr/bib/emprunterLivre` en B ; schéma importé (`?xsd=1`) en A contre schéma intégré en B ; deux opérations en A, une seule en B.

**2. Si on renomme `dateRetour` dans le code, quel contrat change ? Quel risque pour les clients ?**

Le projet **code-first (A)** : le WSDL est régénéré à partir de la classe, et l'élément `dateRetour` change de nom sans que personne ne modifie le contrat volontairement. Les clients générés à partir de l'ancien WSDL ne trouvent plus l'élément (valeur `null` ou erreur de désérialisation) : le contrat est rompu sans prévenir.
En **contract-first (B)**, le WSDL ne change pas ; le code doit suivre le contrat.

**3. Quelle approche pour le service Paiement ? Pour un prototype de deux jours ?**

- **Paiement**, appelé par Scolarité (.NET), Bibliothèque (PHP) et le portail (Node.js) : **contract-first**. Plusieurs équipes et langages partagent le contrat ; il doit être stable, écrit et versionné indépendamment du code Java, et chaque équipe génère son client à partir du même WSDL.
- **Prototype interne de deux jours** : **code-first**. C'est plus rapide, il n'y a qu'un consommateur, et le WSDL généré suffit. Il faudra figer le WSDL si le prototype part en production.
