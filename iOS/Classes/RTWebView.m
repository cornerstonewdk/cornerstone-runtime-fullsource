/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at
 
 http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

#import "RTWebView.h"


@implementation RTWebView


- (void)loadRequest:(NSURLRequest *)request
{
    //self.scrollView.scrollEnabled = NO; 
    //self.scrollView.bounces = NO;    
    //self.scrollView.showsHorizontalScrollIndicator = false;
    //self.scrollView.showsVerticalScrollIndicator = false;
	[super loadRequest:request];
}

/*
// Only override drawRect: if you perform custom drawing.
// An empty implementation adversely affects performance during animation.
- (void)drawRect:(CGRect)rect {
    // Drawing code.
}
*/




- (void)dealloc {
    [super dealloc];
}


@end