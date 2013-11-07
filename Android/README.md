### COPYRIGHT(C) 2012 BY SKTELECOM CO., LTD. ALL RIGHTS RESERVED ###

----------

- 본 프로젝트는 Cornerstone Runtime Android Full Source 이며 eclipse 프로젝트이기 때문에 clone 후에 
빌드하여 바로 테스트가 가능하다. 

### 1 Android Runtime 웹앱 개발 환경 구조 

1) src - Cornerstone Runtime의 Native Code 및 plugin 개발시에 작성하는 Java Native Code

2) asset - 실제 웹앱의 웹 리소스(HTML/CSS/JS/IMG) 와 Runtime JavaScript Library 가 저장되는 위치 
	
-	**assets/www** : 웹앱의 저장 위치 
-	**assets/www/index.html** : 웹앱의 첫 실행 파일 (RuntimeStandAlone.java 에서 변경가능) 

3) drawable - device에 보여지는 icon 및 splash image를 저장하는 위치 

-	**icon.png** : device에 보여지는 icon
-	**splash.png** : 웹앱 실행 초기에 보여지는 splash image (optional) 

4) values , xml - 웹앱의 name 및 runtime setting을 할 수 있는 폴더 

-	**value/string.xml** : 웹앱의 name을 세팅하는 파일 
-	**xml/config.xml** : device의 orientation(portrait , landscape , auto) 및 splash image , 메뉴 사용여부 , loading progressbar 사용여부를 세팅 하는 파일 

5) AndroidManifest.xml : 하나의 Native Application으로써의 고유한 Package 명을 지정하는 파일, 하드웨어 가속 GPU 렌더링 사용여부를 지정하는 파일.
