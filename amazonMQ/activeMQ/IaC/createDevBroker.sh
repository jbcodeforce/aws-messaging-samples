aws mq create-broker --broker-name demo-jb \
   --no-auto-minor-version-upgrade \
   --authentication-strategy SIMPLE \
   --deployment-mode SINGLE_INSTANCE \
   --engine-type ACTIVEMQ \
    --engine-version 5.17.6 \
    --host-instance-type mq.t3.micro \
    --publicly-accessible \
    --users "Username=demouser,Password=userpassw0rd,Groups=admin,ConsoleAccess=true" \
    --logs Audit=false,General=true \
    --region us-west-2 \
 