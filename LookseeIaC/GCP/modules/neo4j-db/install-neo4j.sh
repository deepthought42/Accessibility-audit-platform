#!/bin/bash
wget -O - https://debian.neo4j.com/neotechnology.gpg.key | sudo apt-key add -
echo 'deb https://debian.neo4j.com stable 5' | sudo tee /etc/apt/sources.list.d/neo4j.list
sudo apt-get update
sudo apt-get install -y neo4j

# Ensure SSH daemon is enabled and started
sudo systemctl enable ssh
sudo systemctl start ssh

sudo ufw allow 22
sudo ufw allow 7474
sudo ufw allow 7687

# Download the CQL file to /
curl -o /home/neo4j/create-indexes-and-constraints.cql https://raw.githubusercontent.com/deepthought42/LookseeIaC/refs/heads/main/create-indexes-and-constraints.cql

# Set initial password for neo4j admin user
sudo systemctl stop neo4j
sudo -u neo4j neo4j-admin set-initial-password '${var.neo4j_password}'
sudo systemctl enable neo4j
sudo systemctl start neo4j

# Create a new user with the specified username and password
sudo -u neo4j neo4j-admin create-user ${var.neo4j_username} '${var.neo4j_password}'

# create database
sudo -u neo4j neo4j-admin create-database ${var.neo4j_db_name}

# run file to create indexes and constraints
sudo -u neo4j neo4j-admin run /home/neo4j/create-indexes-and-constraints.cql