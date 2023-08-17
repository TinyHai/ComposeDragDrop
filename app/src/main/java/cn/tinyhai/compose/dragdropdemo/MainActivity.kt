package cn.tinyhai.compose.dragdropdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.tinyhai.compose.dragdrop.DragDropBox
import cn.tinyhai.compose.dragdrop.DragTarget
import cn.tinyhai.compose.dragdrop.DragType
import cn.tinyhai.compose.dragdrop.modifier.dropTarget
import cn.tinyhai.compose.dragdrop.rememberDropTargetState
import cn.tinyhai.compose.dragdropdemo.ui.theme.ComposeDragDropTheme
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeDragDropTheme {
                val snackbarHostState = remember {
                    SnackbarHostState()
                }
                CompositionLocalProvider(
                    LocalSnackbarHost provides snackbarHostState
                ) {
                    // A surface container using the 'background' color from the theme
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(text = stringResource(id = R.string.app_name)) },
                            )
                        },
                        snackbarHost = {
                            SnackbarHost(snackbarHostState) {
                                Snackbar(snackbarData = it)
                            }
                        }
                    ) {
                        Surface(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(it),
                        ) {
                            DragDropDemo()
                        }
                    }
                }
            }
        }
    }
}

val LocalSnackbarHost =
    compositionLocalOf<SnackbarHostState> { error("LocalSnackbarHost not present") }

data class Food(
    val name: String,
    @DrawableRes val resId: Int,
)

@Preview(widthDp = 360, heightDp = 720)
@Composable
fun DragDropDemo() {
    val foodList = listOf(
        Food("steak", R.drawable.steak),
        Food("pasta", R.drawable.pasta),
        Food("tofu", R.drawable.tofu),
    )
    val animalList = listOf(
        Animal("cat", R.drawable.cat),
        Animal("dog", R.drawable.dog),
        Animal("sheep", R.drawable.sheep),
    )
    DragDropBox(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        defaultDragType = DragType.Immediate,
    ) {
        Column {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(foodList) {
                    FoodItem(food = it)
                }
            }
            LazyRow(
                Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(animalList) {
                    AnimalItem(animal = it)
                }
            }
        }
    }
}

@Preview
@Composable
fun FoodItemPreview() {
    DragDropBox(Modifier.width(360.dp)) {
        FoodItem(food = Food("steak", R.drawable.steak))
    }
}

@Composable
fun FoodItem(food: Food) {
    Card {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            DragTarget(dataToDrop = food.name, modifier = Modifier.size(80.dp)) {
                Image(
                    painter = painterResource(id = food.resId),
                    contentDescription = food.name,
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = food.name,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

data class Animal(
    val name: String,
    @DrawableRes val avatar: Int
)

@Preview
@Composable
fun AnimalItemPreview() {
    DragDropBox {
        AnimalItem(animal = Animal("cat", R.drawable.cat))
    }
}

@Composable
fun AnimalItem(animal: Animal) {
    val scope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHost.current
    val dropTargetState = rememberDropTargetState<String>()
    val (isInBound, data) = dropTargetState
    Card(
        modifier = Modifier.dropTarget(dropTargetState) {
            scope.launch {
                it?.let { food ->
                    snackbarHost.showSnackbar("${animal.name} ate the $food")
                }
            }
        },
        colors = CardDefaults.cardColors(
            containerColor = if (isInBound) MaterialTheme.colorScheme.surfaceTint else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            Modifier
                .wrapContentSize()
                .padding(8.dp),
        ) {
            Image(
                painter = painterResource(id = animal.avatar),
                contentDescription = animal.name,
                Modifier.size(60.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isInBound) data.toString() else animal.name,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}