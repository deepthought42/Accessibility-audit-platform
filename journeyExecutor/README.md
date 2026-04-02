# Journey Evaluator
Executes journeys and extract journey related data as well as validating/invalidating journeys


# Getting Started

## Launch Jar locally


### Command Line Interface(CLI)

mvn clean install

java -ea -jar target/Look-see-#.#.#.jar

NOTE: The `-ea` flag enables Java assertions at runtime. This project uses **Design by Contract (DbC)** and assertions **must** be enabled in all environments (development, staging, and production) to enforce preconditions and postconditions.

# Design by Contract

This project follows Design by Contract conventions. Every non-trivial method documents and enforces:

- **Preconditions (`@pre`)** -- what must be true when the method is called (caller's responsibility). Enforced with `assert` statements at the top of the method body.
- **Postconditions (`@post`)** -- what the method guarantees when it returns (method's responsibility). Enforced with `assert` statements before the return value.

Contracts are documented in Javadoc using `@pre` and `@post` tags and enforced at runtime via Java `assert` statements. Always run with `-ea` to activate them.

### Neo4j application setup

Note that this section will need to be replaced once we have an Anthos or Terraform script. 

Step 1: setup firewall for neo4j

```bash
gcloud compute firewall-rules create allow-neo4j-bolt-http-https --allow tcp:7473,tcp:7474,tcp:7687 --source-ranges 0.0.0.0/0 --target-tags neo4j
```

Step 2: Get image name for Community version 1.4
```bash
 	gcloud compute images list --project launcher-public | grep --extended-regexp "neo4j-community-1-4-.*"
```

Step 3: create new instance
```bash
gcloud config set project cosmic-envoy-280619
gcloud compute instances create neo4j-prod --machine-type e2-medium --image-project launcher-public --image neo4j-community-1-4-3-6-apoc --tags neo4j,http-server,https-server

gcloud compute instances add-tags neo4j-stage --tags http-server,https-server
```

Step 4 : SSH to server and check status
```bash
gcloud compute ssh neo4j-stage
sudo systemctl status neo4j
```

Follow step 3 from this webpage to configure neo4j server - https://www.digitalocean.com/community/tutorials/how-to-install-and-configure-neo4j-on-ubuntu-20-04

Step 6: Delete neo4j instance
```bash
gcloud compute instances delete neo4j-stage
```


### Docker

```bash
mvn clean install

docker build --tag look-see .

docker run -p 80:80 -p 8080:8080 -p 9080:9080 -p 443:443 --name look-see look-see
```

### Deploy docker container to gcr
```bash
gcloud auth print-access-token | sudo docker login   -u oauth2accesstoken   --password-stdin https://us-central1-docker.pkg.dev

sudo docker build --no-cache -t us-central1-docker.pkg.dev/cosmic-envoy-280619/journey-executor/#.#.# .

sudo docker push us-central1-docker.pkg.dev/cosmic-envoy-280619/journey-executor/#.#.#


sudo docker build --no-cache -t us-central1-docker.pkg.dev/cosmic-envoy-280619/journey-executor/journey-executor .

sudo docker push us-central1-docker.pkg.dev/cosmic-envoy-280619/journey-executor/journey-executor 
```

# Security

## Generating a new PKCS12 certificate for SSL

Run the following command in Linux to create a keystore called api_key with a privateKeyEntry

```bash
openssl pkcs12 -export -inkey private.key -in certificate.crt -out api_key.p12
```
# Testing

## Sending url message

	log in to Google Cloud console and navigate to PubSub service
	Under topics find the URL Topic and select "Messages" from the sub navigation menu
	
	Send the following message or one that is similar
	
	{
		"domainId": 1,
		"accountId": 5,
		"domainAuditRecordId": 11,
		"pageAuditId": -1,
		"url": "look-see.com"
	}
	
Migration notes:

	01-06-2023: Replace isSecure property with secured property in PageState objects
		
		MATCH (n:PageState) SET n.secured=n.isSecure RETURN n
		MATCH (n:PageState) SET n.isSecure=NULL RETURN n
		
		
# Selenium standalone server deployment

1. Pull Selenium docker image
2. Push Docker image to GCP artifact repository
3. Tag docker image
	
	```bash
		sudo docker image tag selenium/standalone-chrome:114.0 us-central1-docker.pkg.dev/cosmic-envoy-280619/selenium-chrome/114.0
	```
		
4. Create Cloud Run service
		
- Environment variable config
			
			
		