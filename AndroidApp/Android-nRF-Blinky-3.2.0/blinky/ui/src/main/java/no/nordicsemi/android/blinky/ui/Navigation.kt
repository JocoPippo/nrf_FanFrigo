package no.nordicsemi.android.blinky.ui
/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material.icons.twotone.Assistant
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import no.nordicsemi.android.blinky.ui.control.view.CamperFanControlView
import no.nordicsemi.android.blinky.ui.control.view.PasswordControlView
import no.nordicsemi.android.blinky.ui.control.view.PasswordViewModel
import no.nordicsemi.android.blinky.ui.control.view.SettingsViewModel
import no.nordicsemi.android.blinky.ui.control.viewmodel.CamperFanViewModel

@Composable
fun MainScreen(viewModel: CamperFanViewModel) {

    val temperatureValue by viewModel.temperatureValue.collectAsStateWithLifecycle()
    val thresholdValue by viewModel.thresholdValue.collectAsStateWithLifecycle()
    val dutycleValue by viewModel.dutyCycleValue.collectAsStateWithLifecycle()
    val fan2Value by viewModel.fan2Value.collectAsStateWithLifecycle()
    val powerOnState by viewModel.powerOnState.collectAsStateWithLifecycle()
    val ledsState by viewModel.ledsOnState.collectAsStateWithLifecycle()
    //LPP TODO modify
    val dutyViewModel: SettingsViewModel = viewModel<SettingsViewModel>()
    var dutyUnused by remember { mutableStateOf(dutycleValue) }

    CamperFanControlView(
        temperatureValue = temperatureValue,
        thresholdValue = thresholdValue,
        dutyCycleValue = dutycleValue,
        fan2Value = fan2Value,
        powerOnState = powerOnState,
        onPowerOnChanged = { viewModel.powerOn(it) },
        ledsState = ledsState,
        ledsChanged = { viewModel.ledsOn(it) },
        settingsViewModel = dutyViewModel,
        dutyUnused = dutyUnused,
        dutySetCallback = { force, value -> viewModel.setDutyCycle(force, value) },
        thresholdCallback = { force, value -> viewModel.setThreshold(force, value) },
        fanCallback = { force, value -> viewModel.setFan2(force, value) },
        modifier = Modifier
            .widthIn(max = 460.dp)
            .verticalScroll(rememberScrollState())
            .padding(2.dp),

    )

}

@Composable
fun PassowrdScree(viewModel: CamperFanViewModel) {
    val pswViewModel: PasswordViewModel = viewModel<PasswordViewModel>()
    val modifier = Modifier

    PasswordControlView(
        modifier = modifier,
        pswCallback = {value -> viewModel.updatePassword(value) },
        pswViewModel = pswViewModel,
    )
}

enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
) {
    STATUS("status", "Fan Status", Icons.TwoTone.Assistant, "Current Fan operation"),
    SETTINGS(
        "setting",
        "Device Settings",
        Icons.Default.SettingsApplications,
        "Update the device settings"
    ),
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: Destination,
    modifier: Modifier = Modifier,
    viewModel: CamperFanViewModel
) {
    NavHost(
        navController,
        startDestination = startDestination.route
    ) {
        Destination.entries.forEach { destination ->
            composable(destination.route) {
                when (destination) {
                    Destination.STATUS -> MainScreen(viewModel)
                    Destination.SETTINGS -> PassowrdScree(viewModel)
                }
            }
        }
    }
}

@Preview
// [START android_compose_components_navigationbarexample]
@Composable
fun NavigationBarExample(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val startDestination = Destination.STATUS
    var selectedDestination by rememberSaveable { mutableIntStateOf(startDestination.ordinal) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
                Destination.entries.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        selected = selectedDestination == index,
                        onClick = {
                            navController.navigate(route = destination.route)
                            selectedDestination = index
                        },
                        icon = {
                            Icon(
                                destination.icon,
                                contentDescription = destination.contentDescription
                            )
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { contentPadding ->
        AppNavHost(
            navController,
            startDestination,
            modifier = Modifier.padding(contentPadding),
            hiltViewModel()
        )
    }
}
// [END android_compose_components_navigationbarexample]

@Preview
// [START android_compose_components_navigationrailexample]
@Composable
fun NavigationRailExample(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val startDestination = Destination.STATUS
    val viewModel: CamperFanViewModel = hiltViewModel()
    var selectedDestination by rememberSaveable { mutableIntStateOf(startDestination.ordinal) }


    Scaffold(modifier = modifier) { contentPadding ->
        NavigationRail(modifier = Modifier.padding(contentPadding)) {
            Destination.entries.forEachIndexed { index, destination ->
                NavigationRailItem(
                    selected = selectedDestination == index,
                    onClick = {
                        navController.navigate(route = destination.route)
                        selectedDestination = index
                    },
                    icon = {
                        Icon(
                            destination.icon,
                            contentDescription = destination.contentDescription
                        )
                    },
                    label = { Text(destination.label) }
                )
            }
        }
        AppNavHost(navController, startDestination, viewModel = viewModel)
    }
}
// [END android_compose_components_navigationrailexample]

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
// [START android_compose_components_navigationtabexample]
@Composable
fun NavigationTabExample(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val startDestination = Destination.STATUS
    val viewModel: CamperFanViewModel = hiltViewModel()
    var selectedDestination by rememberSaveable { mutableIntStateOf(startDestination.ordinal) }

    Scaffold(modifier = modifier) { contentPadding ->
        PrimaryTabRow(
            selectedTabIndex = selectedDestination,
            modifier = Modifier.padding(contentPadding)
        ) {
            Destination.entries.forEachIndexed { index, destination ->
                Tab(
                    selected = selectedDestination == index,
                    onClick = {
                        navController.navigate(route = destination.route)
                        selectedDestination = index
                    },
                    text = {
                        Text(
                            text = destination.label,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
        AppNavHost(navController, startDestination, viewModel = viewModel)
    }
}
// [END android_compose_components_navigationtabexample]