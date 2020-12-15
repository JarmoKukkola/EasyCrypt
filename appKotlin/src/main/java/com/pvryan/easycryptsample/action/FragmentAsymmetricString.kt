/*
 * Copyright 2018 Priyank Vasa
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pvryan.easycryptsample.action

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pvryan.easycrypt.ECKeys
import com.pvryan.easycrypt.ECResultListener
import com.pvryan.easycrypt.asymmetric.ECAsymmetric
import com.pvryan.easycrypt.asymmetric.ECRSAKeyPairListener
import com.pvryan.easycrypt.asymmetric.ECVerifiedListener
import com.pvryan.easycrypt.symmetric.ECSymmetric
import com.pvryan.easycryptsample.Constants
import com.pvryan.easycryptsample.R
import com.pvryan.easycryptsample.extensions.*
import com.transitionseverywhere.Rotate
import com.transitionseverywhere.TransitionManager
import kotlinx.android.synthetic.main.fragment_asymmetric_string.*
import org.jetbrains.anko.support.v4.onUiThread
import java.io.File
import java.security.KeyPair
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.InvalidKeySpecException

@SuppressLint("SetTextI18n")
class FragmentAsymmetricString : Fragment(), ECResultListener {

    private val eCryptSymmetric = ECSymmetric()
    private val eCryptAsymmetric = ECAsymmetric()
    private val eCryptKeys = ECKeys()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_asymmetric_string, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        edInputS.setOnClickEndDrawableListener {
            /*val intent = Intent(activity, CameraActivity::class.java)
            intent.putExtra(Constants.TITLE, getString(R.string.title_camera))
            startActivityForResult(intent, Constants.rCCameraResult)*/
            llContentAString.snackLong(getString(R.string.scanComingSoon, "encrypt"))
        }

        val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        rlPublicKeyTitleS.setOnLongClickListener {
            val data = ClipData.newPlainText("result", tvPublicKeyS.text)
            clipboard.setPrimaryClip(data)
            view.snackLong("Public key copied to clipboard")
            true
        }
        rlPrivateKeyTitleS.setOnLongClickListener {
            val data = ClipData.newPlainText("result", tvPrivateKeyS.text)
            clipboard.setPrimaryClip(data)
            view.snackLong("Secure private key copied to clipboard")
            true
        }
        rlOutputTitleS.setOnLongClickListener {
            val data = ClipData.newPlainText("result", tvResultS.text)
            clipboard.setPrimaryClip(data)
            view.snackLong("Output copied to clipboard")
            true
        }

        buttonEncryptS.setOnClickListener {

            if (edPasswordS.text.toString() == "") {
                view.snackLong("Password cannot be empty")
                return@setOnClickListener
            }

            val password = edPasswordS.text.toString()
            progressBarS.show()

            // Generate RSA KeyPair
            eCryptKeys.genRSAKeyPair(object : ECRSAKeyPairListener {

                override fun onGenerated(keyPair: KeyPair) {
                    val publicKey = keyPair.public as RSAPublicKey
                    onUiThread { tvPublicKeyS.text = publicKey.encoded.toBase64String() }

                    // Symmetrically encrypt private key
                    val privateKey = keyPair.private as RSAPrivateKey
                    eCryptSymmetric.encrypt(privateKey.encoded.toBase64String(),
                            password, object : ECResultListener {
                        override fun <T> onSuccess(result: T) {
                            onUiThread { tvPrivateKeyS.text = result as String }
                            eCryptAsymmetric.encrypt(edInputS.text.toString(),
                                    publicKey, this@FragmentAsymmetricString)
                        }

                        override fun onFailure(message: String, e: Exception) {
                            onUiThread {
                                progressBarS.hide()
                                tvPrivateKeyS.text = "Error while encrypting private key. $message"
                                view.snackLong("Error while encrypting private key. $message")
                            }
                        }
                    })
                }

                override fun onFailure(message: String, e: Exception) {
                    onUiThread {
                        progressBarS.hide()
                        view.snackLong("Error: $message")
                    }
                }
            })
        }

        buttonDecryptS.setOnClickListener {

            if (edPasswordS.text.toString() == "") {
                view.snackLong("Password cannot be empty")
                return@setOnClickListener
            }
            if (tvPrivateKeyS.text == "") {
                view.snackLong("Encrypt first to generate private key")
                return@setOnClickListener
            }

            val password = edPasswordS.text.toString()
            // Decrypt private key
            eCryptSymmetric.decrypt(tvPrivateKeyS.text, password, object : ECResultListener {
                override fun <T> onSuccess(result: T) {
                    try {
                        val privateKey = eCryptKeys.genRSAPrivateKeyFromBase64(result as String)
                        // Decrypt input text
                        eCryptAsymmetric.decrypt(edInputS.text, privateKey, this@FragmentAsymmetricString)
                        onUiThread { progressBarS.show() }
                    } catch (e: IllegalArgumentException) {
                        onFailure("Not a valid base64 string", e)
                    } catch (e: InvalidKeySpecException) {
                        onFailure("Not a valid private key", e)
                    }
                }

                override fun onFailure(message: String, e: Exception) {
                    onUiThread { view.snackLong("Error while decrypting private key. $message") }
                }
            })
        }

        buttonSignS.setOnClickListener {

            if (edPasswordS.text.toString() == "") {
                view.snackLong("Password cannot be empty")
                return@setOnClickListener
            }

            val password = edPasswordS.text.toString()
            progressBarS.show()

            val sigFile = File(Environment.getExternalStorageDirectory(),
                    "ECryptSample/sample.sig")
            if (sigFile.exists()) sigFile.delete()

            eCryptKeys.genRSAKeyPair(object : ECRSAKeyPairListener {

                override fun onGenerated(keyPair: KeyPair) {
                    onUiThread {
                        tvPublicKeyS.text = (keyPair.public as RSAPublicKey).encoded.toBase64String()
                    }
                    val privateKey = keyPair.private as RSAPrivateKey

                    // Encrypt private key
                    eCryptSymmetric.encrypt(privateKey.encoded.toBase64String(),
                            password, object : ECResultListener {
                        override fun <T> onSuccess(result: T) {
                            eCryptAsymmetric.sign(edInputS.text,
                                    privateKey,
                                    this@FragmentAsymmetricString,
                                    sigFile)
                            onUiThread { tvPrivateKeyS.text = result as String }
                        }

                        override fun onFailure(message: String, e: Exception) {
                            onUiThread {
                                progressBarS.hide()
                                tvPrivateKeyS.text = "Error while encrypting private key. $message"
                                view.snackLong("Error while encrypting private key. $message")
                            }
                        }
                    })
                }

                override fun onFailure(message: String, e: Exception) {
                    onUiThread {
                        progressBarS.hide()
                        view.snackLong("Failed to generate RSA key pair. Try again.")
                    }
                }
            })
        }

        buttonVerifyS.setOnClickListener {

            if (tvPublicKeyS.text == "") {
                view.snackLong("Sign first to generate public key")
                return@setOnClickListener
            }

            try {
                val publicKey = eCryptKeys.genRSAPublicKeyFromBase64(tvPublicKeyS.text.toString())
                eCryptAsymmetric.verify(edInputS.text.toString(), publicKey,
                        File(Environment.getExternalStorageDirectory(), "ECryptSample/sample.sig"),
                        object : ECVerifiedListener {
                            override fun onSuccess(verified: Boolean) {
                                onUiThread {
                                    progressBarS.hide()
                                    if (verified) tvResultS.text = getString(R.string.msg_valid)
                                    else tvResultS.text = getString(R.string.msg_invalid)
                                }
                            }

                            override fun onFailure(message: String, e: Exception) {
                                onUiThread {
                                    progressBarS.hide()
                                    view.snackLong("Error: $message")
                                }
                            }
                        })
            } catch (e: IllegalArgumentException) {
                view.snackLong("Not a valid base64 string")
            } catch (e: InvalidKeySpecException) {
                view.snackLong("Not a valid private key")
            }
        }

        rlPrivateKeyTitleS.setOnClickListener {
            TransitionManager.beginDelayedTransition(it.parent as ViewGroup, Rotate())
            if (tvPrivateKeyS.visibility == View.GONE) {
                bExpandCollapsePrivateS.rotation = 180f
                tvPrivateKeyS.show(true)
            } else {
                bExpandCollapsePrivateS.rotation = 0f
                tvPrivateKeyS.gone(true)
            }
        }

        rlPublicKeyTitleS.setOnClickListener {
            TransitionManager.beginDelayedTransition(it.parent as ViewGroup)
            if (tvPublicKeyS.visibility == View.GONE) {
                bExpandCollapsePublicS.rotation = 180f
                tvPublicKeyS.show(true)
            } else {
                bExpandCollapsePublicS.rotation = 0f
                tvPublicKeyS.gone(true)
            }
        }

        rlOutputTitleS.setOnClickListener {
            TransitionManager.beginDelayedTransition(it.parent as ViewGroup)
            if (tvResultS.visibility == View.GONE) {
                bExpandCollapseOutputS.rotation = 180f
                tvResultS.show(true)
            } else {
                bExpandCollapseOutputS.rotation = 0f
                tvResultS.gone(true)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            Constants.rCCameraResult -> {
                data?.let { edInputS.setText(it.getStringExtra(Constants.INPUT_STRING) ?: "") }
            }
        }
    }

    override fun <T> onSuccess(result: T) {
        onUiThread {
            progressBarS.hide()
            llContentAString.snackLong("Check output")
            tvResultS.text = when (result) {
                is String -> result
                is File -> resources.getString(
                        R.string.success_result_to_file,
                        (result as File).absolutePath)
                else -> "Invalid output. Try again."
            }
        }
    }

    override fun onFailure(message: String, e: Exception) {
        onUiThread {
            progressBarS.hide()
            llContentAString.snackLong("Error: $message")
        }
    }

    companion object {
        fun newInstance(): Fragment = FragmentAsymmetricString()
    }
}
