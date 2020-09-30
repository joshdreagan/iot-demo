import setuptools

with open("README.md", "r") as fh:
    long_description = fh.read()

setuptools.setup(
    name="sim",
    version="0.0.1",
    author="Josh Reagan",
    description="An IoT device simulator",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/joshdreagan/iot-demo",
    packages=setuptools.find_packages(),
    classifiers=[
        "Programming Language :: Python :: 3",
        "License :: OSI Approved :: Apache2 License",
        "Operating System :: OS Independent",
    ],
    python_requires='>=3.6',
    include_package_data=True,
    entry_points='''
        [console_scripts]
        sim-pumpjack=iot.pumpjack.sim:main
    ''',
)