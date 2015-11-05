            # -*- coding: utf-8 -*-
                  
            
import json
import os

            
            
p = os.path.abspath("input_tweet.txt");
            
tweet_data = []
tweet_file = open(p)
output=open('ft1.txt','a')
            
            
for eachline in tweet_file:
    tweet = json.loads(eachline)
    tweet_data.append('tweet')
                
                
            

            
            
            
            
if isinstance(tweet_data,unicode):
    for i in tweet_file:
                        
        x=i.encode('utf-8')
        output.write(x.decode('unicode-escape'))
    else:
            for j in tweet_data:
                output.write(tweet_data)
                      
            output.write(i)
            
            output.close()
            tweet_file.close()       
                    
               
                
                
            
            
            
            
            
            
            
            
            
            
