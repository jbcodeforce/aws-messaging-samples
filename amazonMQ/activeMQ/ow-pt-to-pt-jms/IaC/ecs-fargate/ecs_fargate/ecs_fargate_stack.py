from aws_cdk import (
    # Duration,
    Stack,
    CfnOutput,
    Tags,
    aws_ec2 as ec2, 
    aws_ecs as ecs,
    aws_ecs_patterns as ecs_patterns
    # aws_sqs as sqs,
)
from constructs import Construct

class EcsFargateStack(Stack):

    def __init__(self, scope: Construct, construct_id: str, **kwargs) -> None:
        super().__init__(scope, construct_id, **kwargs)

        vpc=ec2.Vpc.from_lookup(self, "lookup", is_default=True)
        # ECS cluster
        cluster = ecs.Cluster(self, "j9rCluster",vpc=vpc)
        Tags.of(cluster).add("SolutionName","amq-demo")
        Tags.of(cluster).add("Environment","dev")

        # create a task definition with a consumer java app
        consumer_task = ecs.FargateTaskDefinition(self, "CarRideConsumerTask", cpu=256, memory_limit_mib=1024)
        consumer_task.add_container("CarRideConsumerContainer", image=ecs.ContainerImage.from_registry("j9r/amq-jms-consumer"),
            environment={
                "ACTIVEMQ_URL": "ssl://broker-amq-tcp.default.svc:61616",
                "ACTIVEMQ_USERNAME" : "admin"
                }
            )
        Tags.of(consumer_task).add("SolutionName","amq-demo")
        Tags.of(consumer_task).add("Environment","dev")
        
        consumer_task.default_container.add_port_mappings(ecs.PortMapping(container_port=8080))
        
        
        CfnOutput(self, "ConsumerTaskArn", value=consumer_task.task_arn)
        # add fargate service with a java app
        consumer_service=ecs_patterns.ApplicationLoadBalancedFargateService(self, "CarRideConsumerService",
            cluster=cluster,            # Required
            cpu=256,                    # Default is 256
            desired_count=1,            # Default is 1
            task_image_options=ecs_patterns.ApplicationLoadBalancedTaskImageOptions(
                    image=ecs.ContainerImage.from_registry("j9r/amq-jms-consumer"),
                    environment={
                        "ACTIVEMQ_URL": "ssl://broker-amq-tcp.default.svc:61616",
                        "ACTIVEMQ_USERNAME" : "admin"
                        }
                    ),
            memory_limit_mib=1024,      # Default is 512
            public_load_balancer=True)  # Default is True

        consumer_service.service.connections.security_groups[0].add_ingress_rule(
            peer = ec2.Peer.ipv4(vpc.vpc_cidr_block),
            connection = ec2.Port.tcp(8080),
            description="Allow http inbound from VPC"
        )

        CfnOutput(
            self, "LoadBalancerDNS",
            value=consumer_service.load_balancer.load_balancer_dns_name
        )