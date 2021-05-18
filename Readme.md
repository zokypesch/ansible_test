# how to run
ansible-playbook -i .../inventori/main.ini -e "host={{host name}}" {{template}}.yaml

# example
ansible-playbook -i ../inventori/main.ini -e "host=frontend" -e "digest=1021f54" landing.yaml