package inetsoft.spark.quickbooks.token;

import com.intuit.oauth2.exception.OAuthException;

/**
 * Token strategies provide their own logic for returning an OAuth access token used to query the
 * QuickBooks API
 */
public interface TokenStrategy {
   String getAccessToken() throws OAuthException;
}
