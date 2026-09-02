# Documentation : Rate Limiting distribué sur Redis

Cette documentation explique l'architecture, la configuration et l'utilisation du mécanisme de **Rate Limiting** sur le projet DossierFacile.

---

## 📌 1. Vue d'ensemble

Le mécanisme de Rate Limiting repose sur :
- **Redis** (`StringRedisTemplate`) pour stocker les compteurs de requêtes de façon distribuée (compatible multi-instances).
- **Spring AOP** (`RateLimitAspect`) pour intercepter les appels aux méthodes annotées.
- **Une annotation personnalisée** (`@RateLimit`) déclarative et simple à utiliser sur n'importe quel contrôleur Spring MVC.
- **Gestionnaire HTTP 429** (`RateLimitExceptionHandler`) renvoyant une réponse `429 Too Many Requests` lorsque le quota est dépassé.

Le code du Rate Limiting est situé dans la librairie commune `dossierfacile-common-library` (package `fr.dossierfacile.common.config.ratelimit`), ce qui le rend **immédiatement disponible** pour tous les modules (`dossierfacile-bo`, `dossierfacile-api-tenant`, `dossierfacile-api-watermark`, etc.).

---

## 🚀 2. Comment activer le Rate Limiting sur une route ?

Il suffit de placer l'annotation `@RateLimit` sur la méthode de votre contrôleur Spring MVC.

### Exemple 1 : Limite fixe (ex: 20 requêtes / minute)

```java
import fr.dossierfacile.common.config.ratelimit.RateLimit;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/tenant")
public class TenantController {

    @GetMapping("/document/download/{id}")
    @RateLimit(name = "tenant-download-doc", capacity = 20, period = 1, unit = TimeUnit.MINUTES)
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        return tenantService.downloadDocument(id);
    }
}
```

### Exemple 2 : Limite via `application.properties`

Vous pouvez lier les quotas aux propriétés de votre fichier `application.properties` (avec une valeur par défaut de fallback) :

```java
@GetMapping("/files/{id}")
@RateLimit(
    name = "bo-admin-files",
    perMinuteString = "${ratelimit.bo.admin.files.per.minute:30}",
    perDayString = "${ratelimit.bo.admin.files.per.day:100}"
)
public void getOriginalFileAsByteArray(...) {
    // ...
}
```

Configuration dans `application.properties` :
```properties
ratelimit.bo.admin.files.per.minute=30
ratelimit.bo.admin.files.per.day=100
```

---

## ⚙️ 3. Attributs de l'annotation `@RateLimit`

| Attribut | Type | Description | Valeur par défaut |
|---|---|---|---|
| `name` | `String` | Nom unique du bucket. S'il est vide, le nom de la classe et de la méthode sera utilisé. | `""` |
| `capacity` | `int` | Capacité maximale de requêtes autorisées sur la fenêtre. | `30` |
| `period` | `long` | Durée de la fenêtre temporelle. | `1` |
| `unit` | `TimeUnit` | Unité de temps de la fenêtre (`MINUTES`, `HOURS`, `DAYS`). | `TimeUnit.MINUTES` |
| `perMinute` | `int` | Capacité max par minute (si > 0, prévaut sur `capacity/period`). | `0` |
| `perMinuteString` | `String` | Propriété Spring pour `perMinute` (ex: `"${my.prop:30}"`). | `""` |
| `perDay` | `int` | Capacité max par jour (si > 0, évalué en complément). | `0` |
| `perDayString` | `String` | Propriété Spring pour `perDay` (ex: `"${my.prop:1000}"`). | `""` |

---

## 💡 4. Partage ou séparation des Buckets

Le nom du bucket (`name`) détermine le compteur Redis utilisé :

- **Même nom (`name = "mon-bucket"`) sur plusieurs méthodes** : Les requêtes vers ces différentes routes partagent le **même quota global** par IP.
- **Noms différents (ou `name` omis)** : Chaque route dispose de son **propre quota indépendant** par IP.

---

## 🛡️ 5. Résilience & Mode Fail-Open

- **Sécurité et Haute Disponibilité** : Si Redis est indisponible, hors-ligne ou rencontre une erreur réseau, le service `RedisRateLimiterService` attrape l'exception, écrit un log d'avertissement et autorise la requête (**Fail-Open**). Cela garantit que le rate-limiting ne fait jamais tomber les services métiers.
- **Compatibilité multi-modules** : L'injection de Redis est optionnelle (`@Autowired(required = false)`). Si un module ou un test d'intégration n'a pas Redis configuré, le rate-limiting est automatiquement ignoré sans perturber le démarrage du contexte Spring.

---

## 🔍 6. Clé Redis & Expiration

Chaque incrémentation génère une clé temporaire dans Redis sous la forme :
```
ratelimit:<name>:<IP>:<windowSeconds>:<timestampWindow>
```
Une durée de vie (**TTL**) égale au double de la fenêtre temporelle est automatiquement associée à la clé afin d'assurer le nettoyage automatique de Redis.
