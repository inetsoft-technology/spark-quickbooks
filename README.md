# Spark Quickbooks Connector
Spark SQL 2.3.2 Connector for QuickBooks Online

## Requirements
QuickBooks Online uses OAuth 2 for authorization to query their API. You will need to
create keys as described in the [Authentication and Authorization Guide](https://developer.intuit.com/app/developer/qbo/docs/develop/authentication-and-authorization/oauth-2.0#obtain-oauth2-credentials-for-your-app)

## Installation
Add the JARs as dependencies
#### Spark Data Source
```
groupId: com.inetsoft.connectors
artifactId: spark-quickbooks
version: 1.0.0-SNAPSHOT
classifier: bundle
```

#### QuickBooks Runtime
```
groupId: com.inetsoft.connectors
artifactId: spark-quickbooks-api
version: 1.0.0-SNAPSHOT
```

## Options

|Option|Description|
|---|---|
|authorizationCode|OAuth Authorization Code|
|companyId|QuickBooks Online Company ID|
|clientId|OAuth Client ID|
|clientSecret|OAuthClientSecret|
|redirectUri|HTTPS endpoint for OAuth redirect|
|entity|Object to query from the API|

* `authorizationCode`: Exchanged for access/refresh tokens
* `companyId`: Also called `realmId`, it's the ID of the company that you want to query in QuickBooks
* `clientId`: From your app's **Keys** tab
* `clientSecret`: From your app's **Keys** tab
* `redirectUri`: Also in the **Keys** tab, this need to be HTTPS in production and cannot be localhost
  * Default: `https://developer.intuit.com/v2/OAuth2Playground/RedirectUrl`
* `entity`: Due to the nature of the QuickBooks Online query syntax, only 1 entity may be queried at a time.
We take this entity and pass it as the query `select * from <entity>` and then create a data frame
from the result set to query against with Spark SQL

