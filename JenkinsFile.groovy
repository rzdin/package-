pipeline {
    agent any
    
   
    environment {
        bucket = "sam-jenkins-demo-us-west-2-rana-ziauddin"          //artifact deployment S3 bucket 
        bucket1 = "sam-jenkins-demo-us-east-2-ranaziauddin"         //artifact upload s3 bucket 
        region = "us-east-1"                     //artifact deployment bucket region 
        region1 = "us-east-2"                    //artifact upload bucket region 
        aws_credential = "AWSReservedSSO_AdministratorAccess_564bcbbbca5e5655/rzdin@enquizit.com" //aws credentials 
        application_name = "Nodejs"    
        
           
        
       
       
    } // stages to copy and uzip the artifacts to deploy
     stages{
          stage ('Copy Build Artifact') {         //stage to list artifacts from the artifact upload bucket and to chose the latest build artifact with specific version tag 
           when {
                
                expression { params.TAG_NAME_DEV && params.BRANCH_NAME           //condition has been applied to execute the stage if both the specified parameters are true 
                }
            }
            steps {
                    withAWS(credentials: "${aws_credential}", region: "${region1}"){
                            //This bash script list the artifacts in the s3 artifact ulpload s3 bucket sort the content and copies the latest build artifacts into the jenkins 
                            //working directory 
                            sh'''#!/bin/bash 
                            echo "LINE 1 *****************"
                            
                         
                            rm -rf *.zip
                            rm -r ALL_FILES_IN_BUCKET
                            ls && pwd
                            echo "LINE 2 *****************"
                            aws s3 ls s3://$bucket1/$TAG_NAME_DEV/  --recursive
                            
                      
                            echo "LINE 3 *****************"
                            file_NAME=`(aws s3 ls s3://$bucket1/$TAG_NAME_DEV/  --recursive | sort | tail -n 1 | awk '{print \$4}')`
                           
                            
                            echo "LINE 4 *****************"
                            echo $file_NAME
                      
                            aws s3 cp s3://$bucket1/$file_NAME  . 
                           
                        '''
                        }
                    
                    
                  }
              
                         
              } 
              
         stage('unzipfile'){   //This stage unzips the selected artifact 
            when {
                
                expression { params.TAG_NAME_DEV && params.BRANCH_NAME //condition has been applied to execute the stage if both the specified parameters are true 
                }
            }
            steps{
                     //This shell script makes a directory and then unzip the selected artifact and push the unzipped artifact into the recentlymade directory 
                    sh """   
                    
                    mkdir file_extracted_$TAG_NAME_DEV
                    unzip -o  *_${TAG_NAME_DEV}.zip -d file_extracted_$TAG_NAME_DEV
                    """
                    
                    
          
        }
     }
           
      
      stage('uploading'){ //This stage deploy the artifacts from the copy build artifacts stage to se dployement bucket 
            when {
                
                expression { params.TAG_NAME_DEV && params.BRANCH_NAME     //condition has been applied to execute the stage if both the specified parameters are true
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
                
               
           """
        }  
      }
    }
  } 
}
