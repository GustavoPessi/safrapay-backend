# SafraPay - Backend

Backend Spring Boot do projeto SafraPay (TCC - Gustavo Borges Pessi).

## Stack
- Java 17 (Amazon Corretto)
- Spring Boot 3.3.5 (Web, Data JPA, Validation, Actuator)
- PostgreSQL (Amazon RDS)

## Rodando localmente
Defina as variaveis de ambiente antes de rodar (nunca commitar senha real):

```
DB_URL=jdbc:postgresql://<host>:5432/<database>
DB_USERNAME=postgres
DB_PASSWORD=<senha>
```

```
mvn spring-boot:run
```

## Endpoints de teste
- `GET /api/status` - status simples da aplicacao
- `GET /actuator/health` - health check, inclui teste de conexao com o banco
