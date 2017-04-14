# Manage Kafka Topics

## Install DCOS CLI and Kafka CLI

<pre>
curl -O https://downloads.dcos.io/binaries/cli/linux/x86-64/0.4.15/dcos
chmod +x dcos
./dcos config set core.dcos_url https://m1
./dcos config set core.ssl_verify false
./dcos auth login
Enter the username "admin"
Password: *****
./dcos package install kafka --cli

</pre>

Add home directory to path
<pre>
vi .bash_profile

Append 
:~
to PATH

. .bash_profile

</pre>

You can now execute commands.

- List Topics
dcos kafka topic list  




