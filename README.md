## MarkLogic Java Client API over SSL

## Setup

* Use the Java **keytool** to create a local keystore on your filesystem (this will be a file called **clientkeystore**)

```bash
keytool -keystore clientkeystore -genkey -alias client
```

* Create a Certificate Template in MarkLogic Server in the Admin GUI (**Security** > **Certificate Templates** > **Create** Tab)

![Certificate Template Example](src/main/resources/images/create-certificate-template.png?raw=true "Create Certificate Template")

* Export the Template as a **.crt** file (**Security** > **Certificate Templates** > *Your Certificate Name* > **Status** Tab >  **download** button)


![Certificate Template Example](src/main/resources/images/download-cert.png?raw=true "Create Certificate Template")

* Add the certificate to your **clientkeystore** using keytool:


```bash
keytool -import -keystore clientkeystore -file certificate.crt -alias theCARoot
```

* Configure an Application Server to use the Certificate Template that was just generated (**Configure** > **Groups** > **Default** > **App Servers** > *Your App Server* > **ssl certificate template**):

![Certificate Template Example](src/main/resources/images/set-template-on-appserver.png?raw=true "Create Certificate Template")


* Open and edit **src/main/java/Test.java**:
  * Line 41 specifies the path to your **clientkeystore** file (`FileInputStream mlca = new FileInputStream("src/main/resources/clientkeystore");`)
  * Line 44 contains the password that you set to access the file when you created the clientkeystore (`ks.load(mlca, "test123".toCharArray());`)
  * Lines 61 and 62 specify the hostname, port, username and password to access the MarkLogic Application Server


## To run

Run **Test.java** directly from gradle

```bash
./gradlew run
```

If the connection was successful, you should see evidence that a basic server side eval worked:

```
19:37:43.372 [main] INFO Test - Test Connection (eval 1+1): 2
```

## Further reading

https://docs.oracle.com/cd/E19509-01/820-3503/ggfen/index.html