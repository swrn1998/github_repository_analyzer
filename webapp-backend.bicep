param siteName string
param location string = resourceGroup().location
param planId string
param image string
param acrServer string
param acrUser string
@secure()
param acrPassword string

resource site 'Microsoft.Web/sites@2023-12-01' = {
  name: siteName
  location: location
  properties: {
    serverFarmId: planId
    httpsOnly: true                              // ← satisfies the PG-Deny policy
    siteConfig: {
      linuxFxVersion: 'DOCKER|${image}'
      appSettings: [
        { name: 'WEBSITES_PORT',                     value: '8080' }
        { name: 'SPRING_PROFILES_ACTIVE',            value: 'prod' }
        { name: 'DOCKER_REGISTRY_SERVER_URL',        value: acrServer }
        { name: 'DOCKER_REGISTRY_SERVER_USERNAME',   value: acrUser }
        { name: 'DOCKER_REGISTRY_SERVER_PASSWORD',   value: acrPassword }
      ]
    }
  }
}

output backendUrl string = 'https://${site.properties.defaultHostName}'