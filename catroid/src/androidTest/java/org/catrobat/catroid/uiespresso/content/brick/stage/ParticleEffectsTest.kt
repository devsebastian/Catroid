/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2021 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catrobat.catroid.uiespresso.content.brick.stage

import android.content.Intent
import android.util.Log
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.common.FlavoredConstants
import org.catrobat.catroid.common.LookData
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.StartScript
import org.catrobat.catroid.content.bricks.CloneBrick
import org.catrobat.catroid.content.bricks.FadeParticleEffectBrick
import org.catrobat.catroid.content.bricks.FadeParticleEffectBrick.Companion.FADE_IN
import org.catrobat.catroid.content.bricks.FadeParticleEffectBrick.Companion.FADE_OUT
import org.catrobat.catroid.content.bricks.SetGravityBrick
import org.catrobat.catroid.content.bricks.WaitBrick
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.ui.SpriteActivity
import org.catrobat.catroid.uiespresso.util.actions.CustomActions.wait
import org.catrobat.catroid.uiespresso.util.rules.FragmentActivityTestRule
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.java.KoinJavaComponent.inject
import java.io.File
import java.io.IOException

class ParticleEffectsTest {

    val projectManager: ProjectManager by inject(ProjectManager::class.java)

    companion object {
        const val PROJECT_NAME = "particleTestProject"
    }

    lateinit var script: Script
    lateinit var sprite: Sprite

    @get:Rule
    val baseActivityTestRule = FragmentActivityTestRule(
        SpriteActivity::class.java,
        SpriteActivity.EXTRA_FRAGMENT_POSITION,
        SpriteActivity.FRAGMENT_SCRIPTS
    )

    @Before
    fun setUp() {
        createProject()
        baseActivityTestRule.launchActivity(Intent())
    }

    @Test
    fun particleEffectFadeInTest() {
        script.addBrick(FadeParticleEffectBrick(FADE_IN))
        onView(ViewMatchers.withId(R.id.button_play)).perform(click())
        assertTrue(projectManager.currentSprite.look.hasParticleEffect)
    }

    @Test
    fun particleEffectFadeOutTest() {
        script.addBrick(FadeParticleEffectBrick(FADE_OUT))
        onView(ViewMatchers.withId(R.id.button_play)).perform(click())
        assertFalse(projectManager.currentSprite.look.hasParticleEffect)
    }

    @Test
    fun particleEffectPauseTest() {
        script.addBrick(FadeParticleEffectBrick(FADE_IN))
        onView(ViewMatchers.withId(R.id.button_play)).perform(click())
        pressBack()
        assertTrue(projectManager.currentSprite.look.isParticleEffectPaused)
    }

    @Test
    fun particleEffectResumeTest() {
        script.addBrick(FadeParticleEffectBrick(FADE_IN))
        onView(ViewMatchers.withId(R.id.button_play)).perform(click())
        pressBack()
        onView(ViewMatchers.withId(R.id.stage_dialog_button_continue)).perform(click())
        assertFalse(projectManager.currentSprite.look.isParticleEffectPaused)
    }

    @Test
    fun particleEffectGravityTest() {
        script.addBrick(FadeParticleEffectBrick(FADE_IN))
        script.addBrick(WaitBrick(400))
        script.addBrick(SetGravityBrick(Formula(0.0), Formula(20.0)))

        onView(ViewMatchers.withId(R.id.button_play)).perform(click())
        onView(ViewMatchers.isRoot()).perform(wait(100))

        val look = projectManager.currentSprite.look
        val emitter = look.particleEffect.emitters[0]
        var particleGravity: Double?

        particleGravity = emitter.gravity.highMax.toDouble()
        assertEquals(particleGravity, -10.0, 0.00)

        onView(ViewMatchers.isRoot()).perform(wait(500))

        particleGravity = emitter.gravity.highMax.toDouble()
        assertEquals(particleGravity, 20.0, 0.0)
    }

    @Test
    fun particleEffectOnCloneTest() {
        script.addBrick(FadeParticleEffectBrick(FADE_IN))
        script.addBrick(CloneBrick())
        onView(ViewMatchers.withId(R.id.button_play)).perform(click())

        val sprites = StageActivity.stageListener.spritesFromStage
        var cloneFound = false
        for (sprite in sprites) {
            if (sprite.isClone) {
                cloneFound = true
                assertTrue(sprite.look.hasParticleEffect)
            }
        }

        if (!cloneFound) {
            Assert.fail("No Clone Found")
        }
    }

    @Test
    fun particleAfterWaitTest() {
        script.addBrick(WaitBrick(2000))
        script.addBrick(FadeParticleEffectBrick(FADE_IN))
        onView(ViewMatchers.withId(R.id.button_play)).perform(click())

        val projectManager = projectManager
        val look = projectManager.currentSprite.look

        assertFalse(look.hasParticleEffect)
        onView(ViewMatchers.isRoot()).perform(wait(2000))
        assertTrue(look.hasParticleEffect) // particle effect initialised after 2 seconds
    }

    @Test
    fun particleEffectVisibleWhenNoBackgroundTest() {
        script.addBrick(FadeParticleEffectBrick(FADE_IN))
        onView(ViewMatchers.withId(R.id.button_play)).perform(click())
        assertFalse(sprite.look.isAdditive) // not additive when no background available
    }

    @Test
    fun enableAdditiveByDefaultWhenBackgroundLookAvailableTest() {
        projectManager.currentlyEditedScene.backgroundSprite.lookList.add(LookData())
        script.addBrick(FadeParticleEffectBrick(FADE_IN))
        onView(ViewMatchers.withId(R.id.button_play)).perform(click())
        assertTrue(sprite.look.isAdditive) // additive by default when background available
    }

    @After
    fun tearDown() {
        try {
            StorageOperations.deleteDir(
                File(
                    FlavoredConstants.DEFAULT_ROOT_DIRECTORY,
                    PROJECT_NAME
                )
            )
        } catch (e: IOException) {
            Log.d(javaClass.simpleName, Log.getStackTraceString(e))
        }
    }

    private fun createProject() {
        val project = Project(getApplicationContext(), PROJECT_NAME)
        sprite = Sprite("testSprite")
        script = StartScript()
        sprite.addScript(script)
        project.defaultScene.addSprite(sprite)
        projectManager.currentProject = project
        projectManager.currentSprite = sprite
        projectManager.currentlyEditedScene = project.defaultScene
    }
}
