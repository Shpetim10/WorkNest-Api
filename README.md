# WorkNest-Api
This is the repository for Worknest-APIs, the platform for HR Management System

## Mail setup

The application sends mail through the Resend Java SDK.

Set these environment variables:

```env
RESEND_API_KEY=re_your_resend_api_key
MAIL_FROM_ADDRESS=onboarding@your-domain.com
MAIL_FROM_NAME=WorkNest
MAIL_REPLY_TO=support@your-domain.com
```

Make sure the `MAIL_FROM_ADDRESS` domain is verified in Resend before sending production mail.
