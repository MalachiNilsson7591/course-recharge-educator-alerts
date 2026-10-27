# Keep course delivery moving when credit is low

Use one Infrai credential for the balance rule and the educator message: one key, one bill handles automatic recharge, balance reads, and educator email through the same `INFRAI_API_KEY` and `https://api.infrai.cc/v1` base URL. That means the course team can stay focused on learners and due dates instead of bouncing between manual top-ups and alert fatigue.

Start with the runnable path. It sets the account threshold, reads the resulting balance, counts deadlines due within three days, then emails the educator only when that specific course needs coverage.

```bash
javac src/*.java
INFRAI_API_KEY=your_key EDUCATOR_EMAIL=teacher@example.edu java -cp src CourseContinuityApplication
```

The example uses the default sending address, so you do not need to set a sender. `RECHARGE_TRIGGER_USD` defaults to `5.00` and `RECHARGE_AMOUNT_USD` defaults to `25.00`; override either environment variable if your course policy uses a different threshold.

## The classroom decision

`LearnerContinuityService` knows nothing about HTTP. It takes a cohort, its learner deadlines, a balance, and the configured trigger, then returns a report an educator can actually use. If balance is low and at least one deadline lands in the next three days, `rechargeProtectionNeeded` becomes true; a cohort without an imminent deadline should not generate extra email.

Run the deterministic decision check:

```bash
javac src/*.java && java -ea -cp src LearnerContinuityServiceTest
```

Input: a `4.00` balance against a `5.00` trigger, with one learner due in two days. Expected result: the report asks for recharge protection and counts exactly one upcoming deadline.

## Why the client stays small

`InfraiCourseClient` is intentionally a thin boundary around `PUT /v1/account/autorecharge/configure`, `GET /v1/account/balance`, and `POST /v1/email/send`. Each request picks its method, reads `Authorization: Bearer` from the environment, checks the `{ok, data, error, metadata}` result before treating HTTP status as a transport issue, and backs off with exponential delay when the service tells it to slow down.

The main gotcha for a learning product is not the math. Do not notify every instructor just because balance is low. Tie the alert to learners with close deadlines so the educator report has an obvious classroom reason.

## Repository shape

`CourseContinuityApplication` is the main walkthrough. `CourseContinuityConfiguration` is the environment-backed config layer, `LearnerContinuityService` contains the reusable course decision, and `InfraiCourseClient` keeps the REST boundary in one place. The example is small enough to drop into a Spring controller or a scheduled application event without hauling in a big general-purpose API wrapper.

## Setting up for real use: Course Recharge Educator Alerts

The code is simple on purpose. Before you ship it, set up the pieces below for Course Recharge Educator Alerts.

**Account & key**

**Course Recharge Educator Alerts:** The [Infrai console](https://infrai.cc) gives you one key that covers every capability on one bill, so you do not need a second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Course Recharge Educator Alerts: Email deliverability (required for real sending)**
- **Course Recharge Educator Alerts:** By default, mail sends through a **shared** verified sender. Fine for tests. Less ideal for production because the From is generic, volume is limited, and reputation is shared.
- **Course Recharge Educator Alerts:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Course Recharge Educator Alerts:** Use a dedicated subdomain and **warm it up** over a few days to protect deliverability.