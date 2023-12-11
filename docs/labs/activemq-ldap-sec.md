# Active MQ with LDAP and security lab


## Deploying an OpenLDAP on EC2

1. Create a EC2 instance with Ubuntu, t3.micro, and create a security group with port 22 and HTTP 80, from my IP address.
1. Install Apache HTTPD server

    ```sh
    sudo apt-get update
    sudo apt-get install apache2
    ```
1. Install OpenLDAP with adminpassw0rd

    ```sh
    sudo apt-get install slapd ldap-utils
    ```

1. Start the configuration of LDAP

    ```sh
    sudo dpkg-reconfigure slapd
    # Omit OpenLDAP server configuration? No
    # acme.com as DNS
    # ACME for org
    # Remove the database when slapd is purged? No
    # Move old database?  Yes
    ```

1. Install Web App for OpenLDAP

    ```sh
    sudo apt-get install phpldapadmin
    # update the config
    sudo vi /etc/phpldapadmin/config.php
    # Modify the following:
    # $servers->setValue('server','base',array('dc=acme,dc=com'));
    # $servers->setValue('login','bind_id','cn=admin,dc=acme,dc=com');
    ```
    
1. Go to the admin console: http://ec2-35-86-175-148.us-west-2.compute.amazonaws.com/phpldapadmin/

## Configuring Active MQ jetty to access LDAP

## Configure brokers to access LDAP

## IAM work
