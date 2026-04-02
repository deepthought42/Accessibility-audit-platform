// Create indexes

CREATE INDEX IF NOT EXISTS FOR (var:UXIssueMessage) ON (var.key);
CREATE INDEX IF NOT EXISTS FOR (var:UXIssueMessage) ON (var.category);

CREATE INDEX IF NOT EXISTS FOR (var:ElementState) ON (var.key);
CREATE INDEX IF NOT EXISTS FOR (var:PageState) ON (var.key);
CREATE CONSTRAINT page_key IF NOT EXISTS ON (page:PageState) ASSERT page.key IS UNIQUE;

CREATE INDEX IF NOT EXISTS FOR (var:AuditRecord) ON (var.key);
CREATE INDEX IF NOT EXISTS FOR (var:Step) ON (var.key);
CREATE INDEX IF NOT EXISTS FOR (var:Step) ON (var.candidateKey);

CREATE INDEX IF NOT EXISTS FOR (var:Journey) ON (var.key);
CREATE INDEX IF NOT EXISTS FOR (var:Journey) ON (var.candidateKey);

CREATE INDEX IF NOT EXISTS FOR (var:Audit) ON (var.key);
CREATE INDEX IF NOT EXISTS FOR (var:Audit) ON (var.category);
CREATE INDEX IF NOT EXISTS FOR (var:Audit) ON (var.subcategory);
CREATE INDEX IF NOT EXISTS FOR (var:Audit) ON (var.name);
CREATE INDEX IF NOT EXISTS FOR (var:Audit) ON (var.url);

CREATE INDEX IF NOT EXISTS FOR (var:Account) ON (var.subscription_type);
CREATE INDEX IF NOT EXISTS FOR (var:Account) ON (var.customer_token);
CREATE INDEX IF NOT EXISTS FOR (var:Account) ON (var.api_token);

CREATE INDEX IF NOT EXISTS FOR (var:Domain) ON (var.url);
CREATE INDEX IF NOT EXISTS FOR (var:PageState) ON (var.url);
CREATE INDEX IF NOT EXISTS FOR (var:ElementState) ON (var.classification);
CREATE INDEX IF NOT EXISTS FOR (var:ElementState) ON (var.xpath);

CREATE INDEX IF NOT EXISTS FOR (var:DomainAuditRecord) ON (var.key);
CREATE INDEX IF NOT EXISTS FOR (var:PageAuditRecord) ON (var.key);
CREATE INDEX IF NOT EXISTS FOR (var:ImageElementState) ON (var.key);
CREATE INDEX IF NOT EXISTS FOR (var:LandingStep) ON (var.key);
CREATE INDEX IF NOT EXISTS FOR (var:SimpleStep) ON (var.key);

// LIST ALL NEO4j DEFINED INDEXES
CALL db.indexes();