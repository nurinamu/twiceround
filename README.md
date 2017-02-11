# TWICE-Round

구글 Chrome 브라우저의 New Tab 배경화면을 Twice 이미지로 적용하는 Chrome Extension입니다.
순전히 덕심을 통해서 만들어진 extension으로 상업적 사용이 아닌 개인용도로 만들어졌으며 혹여나 같은 마음의 원스팬분들도 사용할 수 있게 사용법과 코드를 공개합니다.
아직 설치가 간단하지 않으나 기능이 어느 정도 안정화되고 좀 UX가 이뻐지면 패키징해서 Store에 등록 예정입니다.


## 사전 요구사항
- [Chrome Browser](https://www.google.com/chrome/browser/desktop/index.html)
    - 이건 Chrome Extension이기 때문에;
- [Google Search Engine ID](https://cse.google.com/all)
    - Google Custom Search Engine을 하나 생성해야합니다.
    - 생성시에 Search할 사이트를 입력해야하는데 현재 제가 입력한 사이트 조건은 아래와 같습니다
        - *.tistory.com
        - blog.naver.com/*
        - imgur.com
    - CSE 콘솔에서 생성한 Engine의 Public URL을 보면 `cx` param 값이 있는데 이것이 `CSE ID` 입니다.
- [Google API Key](https://console.developers.google.com/apis/credentials)
    - TWICE-Round는 Google Custom Search Engine API를 사용하고 있기 때문에 해당 API Key가 있어야만 사용가능합니다.
- [Webpack](https://webpack.js.org/)
    - build환경을 Webpack을 사용했음. 처음 써보느라 계속 이것저것 건들면서 하게될 듯

## 기능
- Chrome 새탭을 여는 경우 배경이미지 표시
- Portrait / Landscape에 따라 맞춤 배경이미지 표시
- 캐싱된 이미지 링크 목록 미리 보기 / 선택
- 캐싱 이미지 삭제 (구글 검색을 매번하는 것이 아니고 특정 갯수(100개)만 저장하여 반복사용하기 때문에 특정 시점에 수동 갱신해야합니다. ~~아직 자동 갱신구현 안함;~~)

## 설치방법

- 저장소 Clone

    ```git clone git@github.com:nurinamu/twiceround.git```

- 빌드

    ```webpack --config config/webpack.config.js```

- [Chrome Browser 확장프로그램](chrome://extensions)에 등록
    - `압축해제된 확장 프로그램 로드`를 눌러서 설치 폴더 하위의 `app` 폴더를 선택

- 새 탭을 열면 처음 한번 Google API와 Custom Search Engine ID 입력을 하라고 표시됨. 입력하고 `적용` 버튼 누름


