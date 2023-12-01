aws iam create-role --role-name execCommandRole --assume-role-policy-document file://trust-ecs-task.json \
 --description "a role for app to connect to active MQ"

aws iam attach-role-policy --role-name execCommandRole --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy

aws iam put-role-policy --role-name execCommandRole --policy-name accessAMQ --policy-document file://ecs-mq-policy.json