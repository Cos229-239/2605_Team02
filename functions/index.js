const functions = require("firebase-functions");
const stripe = require("stripe")(functions.config().stripe.secret);

// Creates a Payment Intent for a transaction
// Called by the Android app when completing a sale
// Returns a client secret that the app uses to confirm payment

exports.createPaymentIntent = functions.https.onCall(async (data, context) => {
  // Require authentication
  if (!context.auth) {
    throw new functions.https.HttpsError(
        "unauthenticated",
        "Must be logged in to process payments",
    );
  }

  const amount = data.amount; // Amount in cents e.g. 1999 = $19.99
  const currency = data.currency || "usd";

  if (!amount || amount <= 0) {
    throw new functions.https.HttpsError(
        "invalid-argument",
        "Invalid payment amount",
    );
  }

  try {
    // Create a Payment Intent with Stripe
    const paymentIntent = await stripe.paymentIntents.create({
      amount: Math.round(amount), // Must be integer cents
      currency: currency,
      automatic_payment_methods: {
        enabled: true,
      },
    });

    // Return the client secret to the app
    return {
      clientSecret: paymentIntent.client_secret,
      paymentIntentId: paymentIntent.id,
    };
  } catch (error) {
    throw new functions.https.HttpsError(
        "internal",
        error.message,
    );
  }
});
