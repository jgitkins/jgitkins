#java -jar mybatis-generator-core-1.4.2.jar -configfile ./mbg-config-model.xml -overwrite -verbose



#java -jar mybatis-generator-core-custom-1.4.3.jar -configfile ./mbg-config-model.xml -overwrite -verbose
#java -jar mybatis-generator-core-custom-1.4.3.jar -configfile ./mbg-config-command-mapper.xml -overwrite -verbose
#java -jar mybatis-generator-core-custom-1.4.3.jar -configfile ./mbg-config-query-mapper.xml -overwrite -verbose

java -jar mybatis-generator-core-custom-1.4.3.jar -configfile ./mbg-config.xml -overwrite -verbose
