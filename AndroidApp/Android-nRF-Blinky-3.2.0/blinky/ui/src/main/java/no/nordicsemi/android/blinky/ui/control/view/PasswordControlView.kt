package no.nordicsemi.android.blinky.ui.control.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.twotone.Password
import androidx.compose.material.icons.twotone.SettingsApplications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import no.nordicsemi.android.blinky.ui.R
import java.util.regex.Pattern


class PasswordViewModel : ViewModel() {

    val NUMBER_STRING: String = ("^[0-9]{6,6}$")
    val NUMBER: Pattern = Pattern.compile(NUMBER_STRING)
    var psw by mutableStateOf("000000")
        private set

    val pswHasErrors by derivedStateOf {
        if (psw.isNotEmpty()) {

            !NUMBER.matcher(psw).matches()

        } else {
            true
        }
    }

//    fun updatePsw(input: String) {
//        psw = input
//    }
}


@Composable
internal fun PasswordControlView(
    modifier: Modifier = Modifier,
    pswViewModel : PasswordViewModel,
    pswCallback : (UInt) -> Unit,
    ) {

    var psw by remember { mutableStateOf("123456") }
//    var pswForced by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    imageVector = Icons.TwoTone.SettingsApplications,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                )
                Text(
                    text = stringResource(R.string.camperFan_applicationSettings),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(.3f),
                )
            }

//Password Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
/*
                BasicSecureTextField(
                    state = state,
                    textObfuscationMode =
                        if (showPassword) {
                            TextObfuscationMode.Visible
                        } else {
                            TextObfuscationMode.RevealLastTyped
                        },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
                        .padding(6.dp),
                    decorator = { innerTextField ->

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                 text = stringResource(R.string.camperFan_password_set),
                            )

                            Box(

                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 16.dp, end = 48.dp)
                            ) {
                                innerTextField()
                            }
                            Icon(
                                if (showPassword) {
                                    Icons.Filled.Visibility
                                } else {
                                    Icons.Filled.VisibilityOff
                                },
                                contentDescription = "Toggle password visibility",
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .requiredSize(48.dp).padding(16.dp)
                                    .clickable { showPassword = !showPassword }
                            )
                        }
                    }
                )
*/
                OutlinedTextField(
                    value = psw,
                    //readOnly = !pswForced,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    label = {
                        Text(
                            text = stringResource(R.string.camperFan_password_set),
                        )
                    },

                    onValueChange = { input ->
                        psw = input
                        //pswViewModel.updatePsw(input)
                    },
                    isError = pswViewModel.pswHasErrors,
                    supportingText = {
                        if (pswViewModel.pswHasErrors) {
                            Text("Incorrect psw format.")
                        }
                    },
                    modifier = Modifier
                        .weight(.3f)
                        .padding(0.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.TwoTone.Password,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(0.dp),
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image = if (showPassword)
                            Icons.Filled.Visibility
                        else Icons.Filled.VisibilityOff

                        // Please provide localized description for accessibility services
                        val description = if (showPassword) "Hide password" else "Show password"

                        IconButton(onClick = {showPassword = !showPassword}){
                            Icon(imageVector  = image, description)
                        }
                    }
                )

                OutlinedButton(onClick = { pswCallback(psw.toUInt()) }) {
                    Icon(
                        imageVector = Icons.Outlined.Upload,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .size(26.dp)//.weight(.1f)
                    )
                    Text(
                        "Set",
                    )
                }
            }
        }
    }
}

@Composable
@Preview
private fun PasswordViewModelPreview() {
    val pswViewModel : PasswordViewModel = viewModel < PasswordViewModel>()
    PasswordControlView(
        modifier = Modifier.padding(16.dp),
        pswViewModel = pswViewModel,
        pswCallback = { _: UInt -> }
    )
}


