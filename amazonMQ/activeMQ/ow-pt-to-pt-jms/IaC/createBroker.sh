aws mq create-broker  --broker-name j9r-demo \
   --no-auto-minor-version-upgrade \
   --configuration Id=c-1e012ad7-5374-47fa-b9ee-f71d7f044e7a,Revision=1 \
   --deployment-mode SINGLE_INSTANCE\
   --authentication-strategy SIMPLE \
   --engine-type ACTIVEMQ \
   --engine-version 5.17.6 \
   --host-instance-type mq.t2.micro \
   --publicly-accessible \
   --region us-west-2 \
   --users ConsoleAccess=true,Groups=admin,Password=alongenoughpassw0rd,Username=admin,ReplicationUser=false \
   --logs Audit=false,General=true