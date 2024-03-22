# ComposeDragDrop
Android Jetpack Compose DragDrop library

### Versions

[![Maven Central](https://img.shields.io/maven-central/v/cn.tinyhai.compose/dragdrop.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=g%3Acn.tinyhai.compose+a%3Adragdrop)
![Compatible with Compose](https://img.shields.io/badge/Compose-BOM%3A2023.03.00-brightgreen)

## Install
```kotlin
allprojects {
  repositories {
    //...
    mavenCentral()
  }
}
```

Add dependencies:

For kts
```kotlin
implementation("cn.tinyhai.compose:dragdrop:latest_version")
```
For groovy
```groovy
implementation 'cn.tinyhai.compose:dragdrop:latest_version'
```

## Demo
![Demo](gifs/demo.gif)

## Usage

Write a `DragDropBox` and put your content inside it
```kotlin
DragDropBox(
    modifier = Modifier.fillMaxSize(),
    scale = 1.2, // scale size of drag target
    alpha = 0.9, // alpha of drag target
    defaultDragType = DragType.LongPress, // defaultDragType DragType.LongPress or DragType.Immediate
    state = rememberDragDropState(scale, alpha, defaultDragType), // all states of the component
) {
    // put your content here
}
// or
YourComposable() {
    val state = rememberDragDropState(scale, alpha, defaultDragType)
    CompositionLocalProvider(
        LocalDragDrop provides state    // necessary
    ) {
        Box(modifier = Modifier.attachAsContainer()) {
            YourInnerComposable()
            DragDropOverlay() // make sure it above your content
        }
    }
}
```

Wrap your `@Composable` content that you want to make draggable with `DragTarget`
```kotlin
DragTarget<String>(
    dataToDrop = "dataToDrop",
    dragType = DragType.Immediate, // Specify a dragType for this one
                                   // By default,it will be assigned to the defaultDragType you set earlier
) {
    // put your draggable content here
}
// or
YourComposable() {
    val draggableComposable = @Composable {
        // content
    }
    Box(modifier = Modifier.dragTarget(enable, dataToDrop, dragType, draggableComposable)) {
        draggableComposable()
    }
}
```

Wrap your `@Composable` content that you want to make droppable with `DropTarget`
```kotlin
DropTarget<String>(
    onDrop = {
        // this will be invoked when the data is dropped in this DropTarget
    },
) { isInBound, data ->
    // put your droppable content here
}
// or
val state = rememberDropTargetState()
YourComposable(modifier = Modifier.dropTarget(dropTargetState, enabled, onDrop)) {
    // content
}
```

After all of above, make sure your content is structured as follows
```kotlin
DragDropBox { // the container
    CustomComposable {
        // make sure both DragTarget and DropTarget are inside DragDropBox
        DragTarget<Any> { // or any composable which apply dragTarget Modifier
            // draggable content
        }
        DropTarget<Any>(onDrop = {}) { // or any composable which apply dropTarget Modifier
            // droppable content
        }
    }
}
```

For more details, you can clone this project and run it by yourself.

## Credits

- [Drag_and_drop_jetpack_compose](https://github.com/cp-radhika-s/Drag_and_drop_jetpack_compose) inspired and based

## License
```
   Copyright (c) 2023-present ,TinyHai.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```
