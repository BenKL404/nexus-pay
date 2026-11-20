# Nexus Pay

[![CI](https://github.com/BenKL404/nexus-pay/actions/workflows/ci.yml/badge.svg)](https://github.com/BenKL404/nexus-pay/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-orange)
![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F)
![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-336791)

Passerelle d'agrégation de paiements **Mobile Money** pour le Togo (T-Money, Flooz).
Un marchand intègre une seule API ; Nexus Pay route le paiement vers le bon opérateur et suit son statut jusqu'au bout.

Le projet met l'accent sur ce qu'on attend d'un système de paiement en banque : **ne jamais débiter deux fois, ne jamais perdre une trace, ne jamais faire confiance à une notification non signée**.

## Ce qui est en place

| Garantie | Comment |
|---|---|
| Pas de double débit | En-tête `Idempotency-Key` obligatoire. La même requête rejouée renvoie le même paiement ; la même clé avec un autre contenu est refusée (422). Deux requêtes simultanées : la clé primaire en base fait échouer la seconde (409). |
| Notifications authentiques | Les webhooks des opérateurs sont signés en HMAC-SHA256 sur le corps brut, comparés en temps constant. Signature invalide : 401. |
| Statuts cohérents | `PENDING → SUCCESS / FAILED`, et un statut final ne change plus. Un webhook rejoué est ignoré ; un webhook contradictoire est refusé (409). |
| Montants exacts | Francs CFA (XOF) sans centimes, stockés en entier : aucune erreur d'arrondi. |
| Traçabilité | Journal d'audit en ajout seul, écrit dans la même transaction que l'opération. |
| Erreurs lisibles | Format RFC 9457 (Problem Details) avec un `code` stable. |
| Concurrence | Verrouillage optimiste (`@Version`) sur les paiements. |

## Stack

Java 21 · Spring Boot 3.5 · Spring Data JPA · PostgreSQL 16 · Flyway · JUnit 5 / MockMvc · GitHub Actions

## Démarrer

```bash
docker compose up -d          # PostgreSQL
mvn spring-boot:run           # API sur http://localhost:8080
mvn verify                    # tests (base H2 en mémoire)
```

## API

### Créer un paiement

```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 6f1c2a9e-cmd-1042" \
  -d '{"amount": 5000, "provider": "TMONEY", "payerPhone": "+22890123456", "merchantReference": "CMD-1042"}'
```

`201 Created` avec le paiement en `PENDING`. La même requête rejouée renvoie `200` et l'en-tête `Idempotent-Replayed: true`.

### Consulter un paiement et son audit

```
GET /api/v1/payments/{id}
GET /api/v1/payments/{id}/audit
```

### Notification d'un opérateur (webhook)

```
POST /api/v1/webhooks/{tmoney|flooz}
X-NexusPay-Signature: <hex HMAC-SHA256 du corps avec le secret de l'opérateur>

{"providerReference": "TM-…", "status": "SUCCESS"}
```

## Architecture

```
api/          contrôleurs REST et gestion des erreurs
payment/      entité Payment, règles de transition, service
idempotency/  clés d'idempotence
provider/     adaptateurs opérateurs (simulateurs pour l'instant)
webhook/      vérification de signature et traitement des notifications
audit/        journal d'audit
```

Les opérateurs sont isolés derrière l'interface `PaymentProvider` : brancher la vraie API T-Money ou Flooz ne touche pas au reste du code.

## Feuille de route

- [x] Paiement idempotent, webhooks signés, audit, tests
- [ ] Authentification des marchands (clé API + Spring Security)
- [ ] Pattern outbox : appel opérateur hors transaction, avec reprise automatique
- [ ] Expiration des paiements restés `PENDING`
- [ ] Rapprochement quotidien avec les relevés des opérateurs
- [ ] Clients HTTP réels T-Money / Flooz (sandbox)
- [ ] Tests d'intégration PostgreSQL avec Testcontainers
- [ ] Supervision : métriques Prometheus et tableau de bord Grafana
