# LeasePilot AI

An AI copilot for property managers. Tenants report maintenance problems with a photo, text, or a voice note. An AI agent works out what's wrong, checks the lease and the unit's history, picks a vendor, and drafts the work order and the reply to the tenant. **Nothing is sent until the property manager approves it.**

Started at the Meta Global AI Developer Hackathon (Oct 27 – Nov 4, 2026). All of the code was written from scratch starting on Oct 26, 2026, as the git history shows.

## How it works

1. **Tenant reports a problem.** They can type it, attach a photo, or record a voice note in the browser.
2. **Voice to text.** Muse Voice Transcribe turns the voice note into text.
3. **Understand the issue.** Muse Spark reads the message and looks at the photo. It returns a summary, a category (plumbing, electrical, and so on), an urgency level with a reason, what it sees in the photo, and a short phrase naming the damage.
4. **Find the damage.** SAM 3.1 uses that phrase to segment the damage in the photo. The manager sees it outlined.
5. **Agent investigates.** A Muse Spark agent loop calls tools:
   - `get_lease_clauses`: who is responsible for this repair under the lease?
   - `get_unit_history`: has this happened before in this unit?
   - `find_vendors`: who can fix it?
   - `submit_draft`: the work order and tenant reply, citing the lease clause it relied on.

   The backend enforces the rules. The agent can't submit before it has checked the lease and history, and the clause and vendor it cites must actually exist.
6. **Manager approves.** The draft lands in an approval queue sorted by urgency, with the agent's full work log and reasoning. The manager approves or rejects it with a note. Only an approved reply reaches the tenant.

## Meta models used

| Model | API | What it does here |
|---|---|---|
| `muse-spark-1.3` | `POST /v1/chat/completions` | Vision, structured JSON output (strict schema), and tool calling for the agent |
| `muse-voice-transcribe-1.0` | `POST /v1/asr/transcribe` | Voice notes to text (the browser converts recordings to 16 kHz mono WAV) |
| `sam-3.1` | `POST /v1/responses` | Segments the damage in the tenant's photo; masks are decoded in the browser with [`@meta-sam/parser`](https://www.npmjs.com/package/@meta-sam/parser) |

## Tech stack

- **Backend:** Java 21, Spring Boot 4, Spring Data JPA, MySQL 9
- **Frontend:** React 19, Vite, Tailwind CSS v4

```
frontend/  React app (Vite proxies /api to localhost:8080)
backend/   Spring Boot app (package com.leasepilot.backend)
  ai/        Clients for Muse Spark, Muse Voice Transcribe, and SAM 3.1
  agent/     The tool-calling agent that drafts work orders
  service/   Intake, approvals, file storage, demo reset
  controller/ REST endpoints under /api
  seed/      Demo data loaded into an empty database
```

## Run it locally

### What you need

- Java 21
- Node.js 20.19+ or 22.12+
- MySQL 9
- A Meta Model API key from [dev.meta.ai](https://dev.meta.ai)

### 1. Create the database

In a terminal, run `mysql -u root -p`, then:

```sql
CREATE DATABASE leasepilot;
CREATE USER 'leasepilot'@'localhost' IDENTIFIED BY 'choose-a-password';
GRANT ALL PRIVILEGES ON leasepilot.* TO 'leasepilot'@'localhost';
```

The tables are created by [Flyway](https://documentation.red-gate.com/flyway) migrations the first time the backend starts, and the demo data is added right after.

### 2. Start the backend

Secrets are read from environment variables. They are never stored in the code.

```sh
cd backend
export DB_PASSWORD='choose-a-password'
export META_API_KEY='your-meta-api-key'
./mvnw spring-boot:run
```

It's ready when you see `Started BackendApplication`. It runs on http://localhost:8080. Each AI step is logged in this terminal, so you can watch the calls happen.

### 3. Start the frontend

In a second terminal:

```sh
cd frontend
npm install
npm run dev
```

Open http://localhost:5173.

## Changing the database

Tables are created and changed only by the numbered SQL files in `backend/src/main/resources/db/migration` (`V1__…`, `V2__…`). On startup, Flyway runs any file the database hasn't seen yet. Hibernate just checks that the Java entities match the tables (`ddl-auto=validate`), and refuses to start if they don't.

To change the schema, change the entity **and** add the next `V<n>__what_it_does.sql`. Never edit a migration that has already run.

## Accounts

- **Owners** create an account, then open the **Properties** tab to add their buildings, units, and tenants. For each tenant they enter a name, email, phone, and lease dates and rent.
- **Tenants** can only create an account with an email their owner added. They're connected to their unit and lease automatically.
- **Forgot password** sends a one-time link that expires after 30 minutes. Without email settings, the email is printed in the backend terminal instead of being sent (see the roadmap).
- Logins use a secure session cookie. Passwords are stored as BCrypt hashes, and every change is protected against CSRF.

New tenants get a standard set of lease repair clauses, which the AI agent uses to decide who pays for a repair.

## Using the demo

On the start page, **Try the demo** logs you into a sample account without a password:

- **Dana Whitfield** is the property manager of Oakwood Apartments.
- **Priya Shah (Unit 2B)** has a kitchen sink that has leaked twice before. Report it again and the agent notices the pattern.
- **Marcus Reed (1A)** and **Elena Novak (3C)** are other tenants.
- **Jordan Kim** has been added to Unit 4D by Dana but hasn't signed up. Create a tenant account with `jordan.kim@example.com` to see a tenant get connected automatically.

**Reset demo**, at the bottom of the start page, rebuilds Dana's Oakwood Apartments from scratch: new requests, drafts, photos, and anyone who signed up there are removed. Other owners' data isn't touched. Set `app.demo-enabled=false` to turn demo mode off.

## Status and roadmap

Working today: accounts for owners and tenants, property and tenant setup, and the full report → AI analysis → SAM highlight → agent draft → manager approval flow.

Next:

- [ ] Email verification when a tenant signs up (today anyone who knows an invited email could claim it)
- [ ] Run the AI in the background so submitting is instant
- [ ] Automated tests
- [ ] Upload your own lease PDF instead of the sample clauses
- [ ] Send real emails: create a dedicated Gmail for the app, turn on 2-Step Verification, create an App Password, and start the backend with `MAIL_HOST=smtp.gmail.com`, `MAIL_USERNAME`, `MAIL_PASSWORD` (the App Password), and `MAIL_FROM` set. Until then, emails such as password-reset links are printed in the backend terminal.
- [ ] Send approved replies and work orders by email
- [ ] Before/after photo verification when a job is marked fixed
- [ ] Deployment
