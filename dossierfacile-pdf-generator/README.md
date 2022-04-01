# Dossier-Facile-Pdf-Generator
Dossier Facile PDF Generator


# Install and run

To build: `mvn clean install`
To run: `mvn spring-boot:run `

## Requirements
- running DossierFacile database
- running rabbitMQ service

## Configure

We use Github package to store common library `dossierfacile-common-library` .
Thus you need to authenticate yourself to get it on github package.

Configure the following environement variables: 
```
GITHUB_USERNAME
GITHUB_TOKEN
```
Or use your global maven settings: <code> .m2/settings.xml</code>
```
<settings>
    ...
    <servers>
        <server>
            <id>github</id>
            <username>GITHUB_USERNAME</username>
            <password>GITHUB_TOKEN</password>
        </server>
    </servers>
    ...
</settings>
```