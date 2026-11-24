# Partie B : Développement d’un service SOAP selon l’approche Contract First

## 1. Objectif

L’objectif de cette partie est de développer un service Web SOAP en utilisant l’approche Contract First. Dans cette approche, le contrat WSDL est créé avant le code Java.

## 2. Création du contrat WSDL

Nous avons créé le fichier `EmpruntService.wsdl`.

Ce fichier définit :
- Le service `EmpruntService`.
- L’opération `emprunterLivre`.
- Les paramètres `idAdherent` et `isbn`.
- Le type de retour `EmpruntType`.
- L’exception `LivreIndisponible`.

## 3. Génération des classes Java

Nous avons utilisé l’outil `wsimport` avec Maven pour générer automatiquement les classes Java à partir du fichier WSDL.

Commande utilisée :

```bash
mvn clean generate-sources
```

La génération s’est terminée avec succès.

## 4. Implémentation du service

Nous avons créé la classe `EmpruntServiceImpl` qui implémente l’interface générée `EmpruntPortType`.

Cette classe contient la logique de l’opération `emprunterLivre` :

- Si le livre est disponible, le service retourne un numéro d’emprunt et une date de retour.
- Si le livre n’est pas disponible, le service déclenche une exception SOAP `LivreIndisponible`.

## 5. Démarrage du serveur

Nous avons créé la classe `Serveur` pour publier le service SOAP à l’adresse suivante :

`http://localhost:8080/bib/EmpruntService`

Le fichier WSDL est accessible à l’adresse :

`http://localhost:8080/bib/EmpruntService?wsdl`

Le serveur a démarré correctement.

## 6. Tests du service

Nous avons effectué les tests avec PowerShell en utilisant des requêtes SOAP XML, sans utiliser Postman.

### Test 1 : Livre disponible

Nous avons envoyé une requête avec l’ISBN :

`978-2-07-036822-8`

Résultat : le service retourne une réponse SOAP contenant le numéro d’emprunt `E-2026-0812` et la date de retour.

Le test est réussi.

### Test 2 : Livre indisponible

Nous avons envoyé une requête avec l’ISBN :

`978-2-10-074444-4`

Résultat : le service retourne une erreur SOAP Fault avec le message :

`Livre indisponible`

Les détails de l’erreur contiennent :
- ISBN : `978-2-10-074444-4`
- Date de retour prévue : `2026-12-02`

Le test est réussi.

## 7. Conclusion

Dans cette partie, nous avons développé et testé un service Web SOAP selon l’approche Contract First.

Nous avons créé le contrat WSDL, généré les classes Java, implémenté le service et testé les deux cas : livre disponible et livre indisponible.

Les tests ont confirmé le bon fonctionnement du service SOAP.