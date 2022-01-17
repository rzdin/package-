pipeline {
    agent any
    
   
    environment {
        bucket = "sam-jenkins-demo-us-west-2-rana-ziauddin"
        bucket1 = "sam-jenkins-demo-us-east-2-ranaziauddin"
        region = "us-east-1"
        region1 = "us-east-2"
        aws_credential = "AWSReservedSSO_AdministratorAccess_564bcbbbca5e5655/rzdin@enquizit.com"
        application_name = "Nodejs"
        
           
        
       
       
    }
     stages{
          stage ('Copy Build Artifact') {
           when {
                
                expression { params.TAG_NAME_DEV && params.BRANCH_NAME
                }
            }
            steps {
                    withAWS(credentials: "${aws_credential}", region: "${region1}"){
                           
                            sh'''#!/bin/bash 
                            echo "LINE 1 *****************"
                            aws s3api list-objects --bucket $bucket1 --query 'Objects[?starts_with(Name, `myapp-`) == `true`].Name'
                         
                            rm -rf *.zip
                            rm -r ALL_FILES_IN_BUCKET
                            ls && pwd
                            echo "LINE 2 *****************"
                            aws s3 ls s3://$bucket1  --recursive
                            
                      
                            echo "LINE 3 *****************"
                            file_NAME=`(aws s3 ls s3://$bucket1  --recursive | sort | tail -n 1 | awk '{print \$4}')`
                           
                            
                            echo "LINE 4 *****************"
                            echo $file_NAME
                      
                            aws s3 cp s3://$bucket1/$file_NAME  . 
                           
                        '''
                        }
                    
                    
                  }
              
                         
              } 
              
         stage('unzipfile'){
            when {
                
                expression { params.TAG_NAME_DEV && params.BRANCH_NAME
                }
            }
            steps{
                    sh """
                    
                    mkdir file_extracted_$TAG_NAME_DEV
                    unzip -o  *_${TAG_NAME_DEV}.zip -d file_extracted_$TAG_NAME_DEV
                    """
                    
                    
          
        }
     }
           
       stage('Revert deployement'){
            when {
                
                expression { params.FILENAME && params.BRANCH_NAME
                }
            }
            steps{
                    
                 
                 withAWS(credentials: "${aws_credential}", region: "${region1}"){
                   sh'''#!/bin/bash
                        
                        
                        mkdir ALL_ARTIFACTS 
                       
                    
                        aws s3 ls s3://$bucket1  --recursive
                        aws s3 ls s3://$bucket1  --recursive | sort 
                        echo $ALL_ARTIFACTS
                        
                        aws s3 cp s3://$bucket1  ALL_ARTIFACTS --recursive
                        
                        echo "********LINE 1********"
                  
                        cd ALL_ARTIFACTS
                        ls -1 && pwd
                        for FILE in *; do 
                            if [ "$FILENAME" == $FILE ]
                            then
                               echo $FILE
                               unzip -o $FILE -d revert_deployment
                               
                               
                               
                            else 
                               echo "NOT FOUND"
                            fi 
                            
                        done
                       
                       

                         
                      
               
                    
                    
                    '''
                    
                    
          }
        }
     }
      stage('Revert deployement upload'){
            when {
                
                expression { params.FILENAME && params.BRANCH_NAME
                }
            }
            steps{
                   withAWS(credentials: "${aws_credential}", region: "${region}"){
                     
                       s3Upload(file:"ALL_ARTIFACTS/revert_deployment/", bucket:"${bucket}")
                 
                
                       
                       

                         
                      
               
                    
                    
                   
                    
                    
          }
        }
     }
      stage('uploading'){
            when {
                
                expression { params.TAG_NAME_DEV && params.BRANCH_NAME
                }
            }
            steps{
                   withAWS(credentials: "${aws_credential}", region: "${region}"){
                     
                       s3Upload(file:"file_extracted_$TAG_NAME_DEV", bucket:"${bucket}")
                    
             }
         }
       
     
    post{ 
       always { 
           sh """
                rm -r file_extracted_$TAG_NAME_DEV
                rm -r ALL_ARTIFACTS
               
           """
        }  
      }
    }
  } 
}