package com.worknest.features.subscription.application;

import com.stripe.model.Customer;
import com.stripe.model.Event;

public interface StripeService {

    Customer createCustomer(String email, String companyName, String companyId);

    com.stripe.model.Subscription createSubscriptionWithTrial(String customerId, String priceId, String paymentMethodId);

    void cancelSubscription(String stripeSubscriptionId);

    void updatePaymentMethod(String stripeCustomerId, String paymentMethodId);

    com.stripe.model.PaymentMethod getDefaultPaymentMethod(String stripeCustomerId);

    com.stripe.model.Subscription updateSubscriptionPlan(String stripeSubscriptionId, String newPriceId);

    com.stripe.model.Subscription createSubscriptionImmediate(String stripeCustomerId, String priceId);

    com.stripe.model.billingportal.Session createBillingPortalSession(String customerId, String returnUrl);

    Event constructWebhookEvent(String payload, String sigHeader);
}
